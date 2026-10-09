#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source ops/lib.sh
load_production_env

: "${KEYCLOAK_DB_LEGACY_ADMIN_USER:?Set KEYCLOAK_DB_LEGACY_ADMIN_USER to the current Keycloak PostgreSQL superuser}"

for role in "$KEYCLOAK_DB_LEGACY_ADMIN_USER" "$KEYCLOAK_DB_ADMIN_USER" "$KEYCLOAK_DB_USER"; do
  [[ "$role" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || { echo "Invalid PostgreSQL role name" >&2; exit 1; }
done

compose exec -T postgres-keycloak psql --set=ON_ERROR_STOP=1 \
  --username "$KEYCLOAK_DB_LEGACY_ADMIN_USER" --dbname "$KEYCLOAK_DB_NAME" \
  --set=bootstrap="$KEYCLOAK_DB_ADMIN_USER" \
  --set=bootstrap_password="$KEYCLOAK_DB_ADMIN_PASSWORD" \
  --set=keycloak="$KEYCLOAK_DB_USER" \
  --set=keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L SUPERUSER CREATEDB CREATEROLE INHERIT', :'bootstrap', :'bootstrap_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'bootstrap') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L SUPERUSER CREATEDB CREATEROLE INHERIT', :'bootstrap', :'bootstrap_password') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOBYPASSRLS', :'keycloak', :'keycloak_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'keycloak') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOBYPASSRLS', :'keycloak', :'keycloak_password') \gexec
ALTER DATABASE :"DBNAME" OWNER TO :"keycloak";
ALTER SCHEMA public OWNER TO :"keycloak";
GRANT ALL ON SCHEMA public TO :"keycloak";
SQL

compose exec -T postgres-keycloak psql --tuples-only --no-align \
  --username "$KEYCLOAK_DB_ADMIN_USER" --dbname "$KEYCLOAK_DB_NAME" \
  --command "SELECT rolname || ':' || rolsuper || ':' || rolcreatedb || ':' || rolcreaterole || ':' || rolbypassrls FROM pg_roles WHERE rolname = '$KEYCLOAK_DB_USER'"
