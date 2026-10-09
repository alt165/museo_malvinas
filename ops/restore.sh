#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source "$SCRIPT_DIR/lib.sh"

backup_dir=
confirmed=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --backup-dir) backup_dir=${2:?Missing value for --backup-dir}; shift 2 ;;
    --confirm-restore) confirmed=true; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

[[ "$confirmed" == true ]] || {
  echo "Restore is destructive for the selected target. Re-run with --confirm-restore." >&2
  exit 2
}
[[ -n "$backup_dir" && -d "$backup_dir" ]] || {
  echo "A valid --backup-dir is required" >&2
  exit 2
}

require_command docker
require_command sha256sum
require_command tar
require_command realpath
load_production_env
require_absolute_directory MUSEO_DATA_DIR "${MUSEO_DATA_DIR:?MUSEO_DATA_DIR is required}"

backup_dir=$(realpath "$backup_dir")
for required in museo.dump keycloak.dump storage.tar.gz metadata.txt manifest.sha256; do
  [[ -f "$backup_dir/$required" ]] || { echo "Missing backup component: $required" >&2; exit 1; }
done
(
  cd "$backup_dir"
  sha256sum --check manifest.sha256
)

started_at=$(date +%s)
compose up -d --wait postgres-museo postgres-keycloak >/dev/null
compose stop reverse-proxy frontend backend keycloak >/dev/null 2>&1 || true

compose exec -T postgres-museo dropdb --username "$MUSEO_DB_ADMIN_USER" --if-exists --force "$MUSEO_DB_NAME"
compose exec -T postgres-museo createdb --username "$MUSEO_DB_ADMIN_USER" --owner "$MUSEO_DB_MIGRATOR_USER" "$MUSEO_DB_NAME"
compose exec -T postgres-museo pg_restore \
  --username "$MUSEO_DB_MIGRATOR_USER" --dbname "$MUSEO_DB_NAME" \
  --no-owner --no-acl --exit-on-error < "$backup_dir/museo.dump"
compose exec -T postgres-museo psql --set=ON_ERROR_STOP=1 \
  --username "$MUSEO_DB_MIGRATOR_USER" --dbname "$MUSEO_DB_NAME" \
  --set=runtime="$MUSEO_DB_RUNTIME_USER" <<'SQL'
GRANT USAGE ON SCHEMA public TO :"runtime";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"runtime";
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO :"runtime";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"runtime";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO :"runtime";
SQL

compose exec -T postgres-keycloak dropdb --username "$KEYCLOAK_DB_ADMIN_USER" --if-exists --force "$KEYCLOAK_DB_NAME"
compose exec -T postgres-keycloak createdb --username "$KEYCLOAK_DB_ADMIN_USER" --owner "$KEYCLOAK_DB_USER" "$KEYCLOAK_DB_NAME"
compose exec -T postgres-keycloak pg_restore \
  --username "$KEYCLOAK_DB_USER" --dbname "$KEYCLOAK_DB_NAME" \
  --no-owner --no-acl --exit-on-error < "$backup_dir/keycloak.dump"

storage_dir=$(realpath -m "$MUSEO_DATA_DIR/storage")
previous_storage="$MUSEO_DATA_DIR/storage.pre-restore-$(date -u +%Y%m%dT%H%M%SZ)"
if [[ -d "$storage_dir" ]]; then
  mv "$storage_dir" "$previous_storage"
fi
mkdir -p "$storage_dir"
compose run --rm --no-deps -T --entrypoint tar backend \
  --extract --gzip --file - --directory /app/storage < "$backup_dir/storage.tar.gz"

flyway_table=$(compose exec -T postgres-museo psql --tuples-only --no-align \
  --username "$MUSEO_DB_RUNTIME_USER" --dbname "$MUSEO_DB_NAME" \
  --command "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL" | tr -d '\r')
object_table=$(compose exec -T postgres-museo psql --tuples-only --no-align \
  --username "$MUSEO_DB_RUNTIME_USER" --dbname "$MUSEO_DB_NAME" \
  --command "SELECT to_regclass('public.objetos_museo') IS NOT NULL" | tr -d '\r')
realm_count=$(compose exec -T postgres-keycloak psql --tuples-only --no-align \
  --username "$KEYCLOAK_DB_USER" --dbname "$KEYCLOAK_DB_NAME" \
  --command "SELECT COUNT(*) FROM realm WHERE name = '${KEYCLOAK_REALM:-museo}'" | tr -d '\r')

[[ "$flyway_table" == t && "$object_table" == t ]] || { echo "Museum database verification failed" >&2; exit 1; }
[[ "$realm_count" -ge 1 ]] || { echo "Keycloak realm verification failed" >&2; exit 1; }

compose up -d --wait keycloak backend frontend reverse-proxy >/dev/null

elapsed=$(( $(date +%s) - started_at ))
echo "Restore completed and services healthy in ${elapsed}s."
if [[ -d "$previous_storage" ]]; then
  echo "Previous storage retained at: $previous_storage"
fi
