#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source "$SCRIPT_DIR/lib.sh"

require_command docker
require_command tar
require_command sha256sum
require_command realpath
load_production_env

destination=${BACKUP_DESTINATION:?BACKUP_DESTINATION is required}
require_absolute_directory BACKUP_DESTINATION "$destination"
require_absolute_directory MUSEO_DATA_DIR "${MUSEO_DATA_DIR:?MUSEO_DATA_DIR is required}"

destination_real=$(realpath -m "$destination")
data_real=$(realpath -m "$MUSEO_DATA_DIR")
case "$destination_real/" in
  "$data_real/"*)
    echo "BACKUP_DESTINATION must not be inside MUSEO_DATA_DIR" >&2
    exit 1
    ;;
esac

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_name="backup-$timestamp"
daily_dir="$destination_real/daily"
weekly_dir="$destination_real/weekly"
monthly_dir="$destination_real/monthly"
partial="$destination_real/.partial-$backup_name-$$"
final="$daily_dir/$backup_name"
mkdir -p "$daily_dir" "$weekly_dir" "$monthly_dir" "$partial"

backend_was_running=false
keycloak_was_running=false
while IFS= read -r service; do
  [[ "$service" == backend ]] && backend_was_running=true
  [[ "$service" == keycloak ]] && keycloak_was_running=true
done < <(compose ps --status running --services)

resume_services() {
  if [[ "$keycloak_was_running" == true || "$backend_was_running" == true ]]; then
    services=()
    [[ "$keycloak_was_running" == true ]] && services+=(keycloak)
    [[ "$backend_was_running" == true ]] && services+=(backend)
    compose up -d --wait "${services[@]}" >/dev/null
  fi
}

cleanup_failure() {
  status=$?
  if [[ $status -ne 0 ]]; then
    rm -rf -- "$partial"
    resume_services || true
  fi
  exit "$status"
}
trap cleanup_failure EXIT

# A short maintenance window freezes application, identity and filesystem writes.
compose stop backend keycloak >/dev/null

compose exec -T postgres-museo pg_dump \
  --username "$MUSEO_DB_RUNTIME_USER" --dbname "$MUSEO_DB_NAME" \
  --format custom --compress 6 --no-owner --no-acl > "$partial/museo.dump"

compose exec -T postgres-keycloak pg_dump \
  --username "$KEYCLOAK_DB_USER" --dbname "$KEYCLOAK_DB_NAME" \
  --format custom --compress 6 --no-owner --no-acl > "$partial/keycloak.dump"

storage_dir="$data_real/storage"
mkdir -p "$storage_dir"
compose run --rm --no-deps -T --entrypoint tar backend \
  --create --gzip --file - --directory /app/storage . > "$partial/storage.tar.gz"

flyway_version=$(compose exec -T postgres-museo psql --tuples-only --no-align \
  --username "$MUSEO_DB_RUNTIME_USER" --dbname "$MUSEO_DB_NAME" \
  --command "SELECT COALESCE((SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1), 'none')" | tr -d '\r')
postgres_version=$(compose exec -T postgres-museo psql --tuples-only --no-align \
  --username "$MUSEO_DB_RUNTIME_USER" --dbname "$MUSEO_DB_NAME" \
  --command "SHOW server_version" | tr -d '\r')
app_version=${APP_VERSION:-$(git rev-parse --verify HEAD 2>/dev/null || echo unknown)}

{
  echo "created_utc=$timestamp"
  echo "application_version=$app_version"
  echo "flyway_schema_version=$flyway_version"
  echo "postgresql_version=$postgres_version"
  echo "hostname=$(hostname)"
  echo "consistency=maintenance-window-backend-and-keycloak-stopped"
  echo "contents=museo.dump,keycloak.dump,storage.tar.gz"
} > "$partial/metadata.txt"

(
  cd "$partial"
  sha256sum museo.dump keycloak.dump storage.tar.gz metadata.txt > manifest.sha256
)

mv "$partial" "$final"
resume_services
trap - EXIT

copy_snapshot() {
  local source=$1
  local target_parent=$2
  local target="$target_parent/$backup_name"
  if ! cp --archive --link "$source" "$target" 2>/dev/null; then
    cp --archive "$source" "$target"
  fi
}

day_of_week=$(date -u +%u)
day_of_month=$(date -u +%d)
[[ "$day_of_week" == 7 ]] && copy_snapshot "$final" "$weekly_dir"
[[ "$day_of_month" == 01 ]] && copy_snapshot "$final" "$monthly_dir"

rotate() {
  local directory=$1
  local keep=$2
  local count=0
  while IFS= read -r entry; do
    count=$((count + 1))
    if (( count > keep )); then
      case "$entry" in
        "$directory"/backup-*) rm -rf -- "$entry" ;;
        *) echo "Refusing to rotate unexpected path: $entry" >&2; exit 1 ;;
      esac
    fi
  done < <(find "$directory" -mindepth 1 -maxdepth 1 -type d -name 'backup-*' -print | sort -r)
}

rotate "$daily_dir" "${BACKUP_KEEP_DAILY:-7}"
rotate "$weekly_dir" "${BACKUP_KEEP_WEEKLY:-4}"
rotate "$monthly_dir" "${BACKUP_KEEP_MONTHLY:-12}"

echo "Backup completed: $final"
echo "Copy or replicate $destination_real to independent media; local-only copies do not satisfy disaster recovery."
