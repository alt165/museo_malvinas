#!/usr/bin/env bash

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

load_production_env() {
  ENV_FILE=${ENV_FILE:-.env.production}
  COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.prod.yml}
  [[ -f "$ENV_FILE" ]] || {
    echo "Environment file not found: $ENV_FILE" >&2
    exit 1
  }
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  export ENV_FILE COMPOSE_FILE
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

require_absolute_directory() {
  local label=$1
  local path=$2
  [[ "$path" = /* ]] || {
    echo "$label must be an absolute path: $path" >&2
    exit 1
  }
  mkdir -p "$path"
}

