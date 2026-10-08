#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source "$SCRIPT_DIR/lib.sh"
load_production_env

backup_dir=${1:-}
[[ -n "$backup_dir" && -d "$backup_dir" ]] || {
  echo "Usage: $0 /absolute/path/to/backup-TIMESTAMP" >&2
  exit 2
}
backup_dir=$(realpath "$backup_dir")
(cd "$backup_dir" && sha256sum --check manifest.sha256)

case "${BACKUP_EXTERNAL_MODE:-}" in
  mounted)
    require_absolute_directory BACKUP_EXTERNAL_PATH "${BACKUP_EXTERNAL_PATH:?required}"
    source_device=$(df -P "$backup_dir" | awk 'NR==2 {print $1}')
    target_device=$(df -P "$BACKUP_EXTERNAL_PATH" | awk 'NR==2 {print $1}')
    [[ "$source_device" != "$target_device" ]] || {
      echo "External path is on the same filesystem; refusing to claim an off-host copy" >&2
      exit 1
    }
    cp --archive "$backup_dir" "$BACKUP_EXTERNAL_PATH/"
    copied="$BACKUP_EXTERNAL_PATH/$(basename "$backup_dir")"
    (cd "$copied" && sha256sum --check manifest.sha256)
    ;;
  rsync-ssh)
    require_command rsync
    : "${BACKUP_EXTERNAL_SSH_TARGET:?required}"
    : "${BACKUP_EXTERNAL_SSH_KEY:?required}"
    : "${BACKUP_EXTERNAL_KNOWN_HOSTS:?required}"
    [[ -f "$BACKUP_EXTERNAL_SSH_KEY" && -f "$BACKUP_EXTERNAL_KNOWN_HOSTS" ]] || {
      echo "SSH key and known_hosts must exist outside the repository" >&2
      exit 1
    }
    rsync --archive --checksum --partial \
      --rsh="ssh -i $BACKUP_EXTERNAL_SSH_KEY -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o UserKnownHostsFile=$BACKUP_EXTERNAL_KNOWN_HOSTS" \
      "$backup_dir/" "$BACKUP_EXTERNAL_SSH_TARGET/$(basename "$backup_dir")/"
    ;;
  *)
    echo "Set BACKUP_EXTERNAL_MODE to mounted or rsync-ssh" >&2
    exit 2
    ;;
esac

echo "Off-host replication completed for $(basename "$backup_dir"). A restore drill from that copy remains mandatory."
