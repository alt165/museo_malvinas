#!/usr/bin/env bash
set -euo pipefail

[[ "${KEYCLOAK_DB_USER:?required}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || {
  echo "Invalid PostgreSQL role name in KEYCLOAK_DB_USER" >&2
  exit 1
}
: "${KEYCLOAK_DB_PASSWORD:?required}"

psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=keycloak="$KEYCLOAK_DB_USER" \
  --set=keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
CREATE ROLE :"keycloak" LOGIN PASSWORD :'keycloak_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
ALTER DATABASE :"DBNAME" OWNER TO :"keycloak";
ALTER SCHEMA public OWNER TO :"keycloak";
GRANT ALL ON SCHEMA public TO :"keycloak";
SQL
