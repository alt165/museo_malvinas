#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source "$SCRIPT_DIR/lib.sh"

mode=dry-run
report_dir=
while [[ $# -gt 0 ]]; do
  case "$1" in
    --quarantine-orphans) mode=quarantine; shift ;;
    --report-dir) report_dir=${2:?Missing value for --report-dir}; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

require_command docker
require_command realpath
load_production_env

storage_root=$(realpath -m "${MUSEO_DATA_DIR:?MUSEO_DATA_DIR is required}/storage")
[[ -d "$storage_root" ]] || { echo "Storage root does not exist: $storage_root" >&2; exit 1; }
min_age_hours=${RECONCILE_MIN_AGE_HOURS:-24}
[[ "$min_age_hours" =~ ^[0-9]+$ && "$min_age_hours" -ge 1 ]] || {
  echo "RECONCILE_MIN_AGE_HOURS must be an integer >= 1" >&2
  exit 1
}

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
report_dir=${report_dir:-"$REPO_DIR/reconcile-report-$timestamp"}
mkdir -p "$report_dir"
work_dir=$(mktemp -d)
trap 'rm -rf -- "$work_dir"' EXIT
references="$work_dir/references.tsv"
resolved="$work_dir/referenced-paths.txt"
: > "$resolved"
: > "$report_dir/missing-references.tsv"
: > "$report_dir/orphan-files.txt"
: > "$report_dir/unsafe-references.tsv"
: > "$report_dir/quarantined-files.tsv"

compose exec -T postgres-museo psql --tuples-only --no-align --field-separator=$'\t' \
  --username "$MUSEO_DB_RUNTIME_USER" --dbname "$MUSEO_DB_NAME" --command "
    SELECT 'foto-original', id::text, ruta_almacenamiento FROM fotos_objeto_museo
    UNION ALL SELECT 'foto-publica', id::text, ruta_publica FROM fotos_objeto_museo WHERE ruta_publica IS NOT NULL
    UNION ALL SELECT 'recibo-escaneado', id::text, ruta_relativa FROM recibos_escaneados_objeto_museo
    UNION ALL SELECT 'veterano-imagen', id::text, ruta_relativa FROM veterano_imagen WHERE ruta_relativa IS NOT NULL
    UNION ALL SELECT 'recibo-firmado', id::text, copia_firmada_ruta_almacenamiento
      FROM recibos_ingreso_objeto WHERE copia_firmada_ruta_almacenamiento IS NOT NULL
    ORDER BY 1, 2" > "$references"

resolve_reference() {
  local kind=$1
  local stored=$2
  case "$stored" in
    /app/storage/*) realpath -m "$storage_root/${stored#/app/storage/}" ;;
    /*) printf '%s\n' "INVALID" ;;
    *)
      case "$kind" in
        foto-original|foto-publica|recibo-escaneado|veterano-imagen) realpath -m "$storage_root/object-files/$stored" ;;
        recibo-firmado) realpath -m "$storage_root/signed-receipts/$stored" ;;
        *) realpath -m "$storage_root/$stored" ;;
      esac
      ;;
  esac
}

while IFS=$'\t' read -r kind id stored_path; do
  [[ -n "$stored_path" ]] || continue
  candidate=$(resolve_reference "$kind" "$stored_path")
  case "$candidate" in
    "$storage_root"/*) ;;
    *) printf '%s\t%s\t%s\n' "$kind" "$id" "$stored_path" >> "$report_dir/unsafe-references.tsv"; continue ;;
  esac
  printf '%s\n' "$candidate" >> "$resolved"
  if [[ ! -f "$candidate" ]]; then
    printf '%s\t%s\t%s\n' "$kind" "$id" "$stored_path" >> "$report_dir/missing-references.tsv"
  fi
done < "$references"
sort -u -o "$resolved" "$resolved"

age_minutes=$((min_age_hours * 60))
while IFS= read -r -d '' physical; do
  case "$physical" in
    "$storage_root"/quarantine/*) continue ;;
  esac
  canonical=$(realpath -m "$physical")
  if ! grep -Fqx -- "$canonical" "$resolved"; then
    relative=${canonical#"$storage_root"/}
    printf '%s\n' "$relative" >> "$report_dir/orphan-files.txt"
    if [[ "$mode" == quarantine ]]; then
      target="$storage_root/quarantine/$timestamp/$relative"
      mkdir -p "$(dirname "$target")"
      mv -- "$canonical" "$target"
      printf '%s\t%s\n' "$relative" "quarantine/$timestamp/$relative" >> "$report_dir/quarantined-files.tsv"
    fi
  fi
done < <(find "$storage_root" -type f -mmin "+$age_minutes" -print0)

{
  echo "created_utc=$timestamp"
  echo "mode=$mode"
  echo "minimum_age_hours=$min_age_hours"
  echo "references=$(wc -l < "$references")"
  echo "missing=$(wc -l < "$report_dir/missing-references.tsv")"
  echo "orphans=$(wc -l < "$report_dir/orphan-files.txt")"
  echo "unsafe=$(wc -l < "$report_dir/unsafe-references.tsv")"
  echo "quarantined=$(wc -l < "$report_dir/quarantined-files.tsv")"
} > "$report_dir/summary.txt"

cat "$report_dir/summary.txt"
echo "Report written to: $report_dir"
[[ "$mode" == dry-run ]] && echo "Dry run only. Use --quarantine-orphans explicitly after reviewing the report."
