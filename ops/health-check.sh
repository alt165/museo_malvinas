#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source "$SCRIPT_DIR/lib.sh"

require_command docker
require_command jq
require_command openssl
require_command find
load_production_env

failed=0
expected=(postgres-museo postgres-keycloak keycloak backend frontend reverse-proxy)
status_json=$(compose ps --format json)
for service in "${expected[@]}"; do
  service_json=$(printf '%s\n' "$status_json" | jq -s --arg service "$service" 'map(select(.Service == $service))[0] // {}')
  state=$(printf '%s' "$service_json" | jq -r '.State // "missing"')
  health=$(printf '%s' "$service_json" | jq -r '.Health // "none"')
  if [[ "$state" != running || "$health" != healthy ]]; then
    echo "CRITICAL service=$service state=$state health=$health"
    failed=1
  else
    echo "OK service=$service health=healthy"
  fi
done

usage=$(df -P "${MUSEO_DATA_DIR:?MUSEO_DATA_DIR is required}" | tail -n 1 | tr -s ' ' | cut -d' ' -f5 | tr -d '%')
if (( usage >= ${DISK_USAGE_WARN_PERCENT:-80} )); then
  echo "CRITICAL disk_usage_percent=$usage"
  failed=1
else
  echo "OK disk_usage_percent=$usage"
fi

cert="${TLS_CERT_DIR:?TLS_CERT_DIR is required}/fullchain.pem"
if ! openssl x509 -checkend "$(( ${TLS_WARN_DAYS:-30} * 86400 ))" -noout -in "$cert" >/dev/null; then
  echo "CRITICAL tls_certificate_expires_within_days=${TLS_WARN_DAYS:-30}"
  failed=1
else
  echo "OK tls_certificate_valid_beyond_days=${TLS_WARN_DAYS:-30}"
fi

latest_backup=$(find "${BACKUP_DESTINATION:?BACKUP_DESTINATION is required}/daily" \
  -mindepth 1 -maxdepth 1 -type d -name 'backup-*' -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | cut -d' ' -f2-)
if [[ -z "$latest_backup" ]]; then
  echo "CRITICAL backup=missing"
  failed=1
else
  backup_age_hours=$(( ( $(date +%s) - $(stat -c %Y "$latest_backup") ) / 3600 ))
  if (( backup_age_hours > ${BACKUP_MAX_AGE_HOURS:-30} )); then
    echo "CRITICAL backup_age_hours=$backup_age_hours"
    failed=1
  else
    echo "OK backup_age_hours=$backup_age_hours path=$latest_backup"
  fi
fi

exit "$failed"

