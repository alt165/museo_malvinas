#!/usr/bin/env bash
set -euo pipefail

validate_role() {
  [[ "$2" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || {
    echo "Invalid PostgreSQL role name in $1" >&2
    exit 1
  }
}

: "${MUSEO_DB_MIGRATOR_USER:?required}"
: "${MUSEO_DB_MIGRATOR_PASSWORD:?required}"
: "${MUSEO_DB_RUNTIME_USER:?required}"
: "${MUSEO_DB_RUNTIME_PASSWORD:?required}"
validate_role MUSEO_DB_MIGRATOR_USER "$MUSEO_DB_MIGRATOR_USER"
validate_role MUSEO_DB_RUNTIME_USER "$MUSEO_DB_RUNTIME_USER"

psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=migrator="$MUSEO_DB_MIGRATOR_USER" \
  --set=migrator_password="$MUSEO_DB_MIGRATOR_PASSWORD" \
  --set=runtime="$MUSEO_DB_RUNTIME_USER" \
  --set=runtime_password="$MUSEO_DB_RUNTIME_PASSWORD" <<'SQL'
CREATE ROLE :"migrator" LOGIN PASSWORD :'migrator_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
CREATE ROLE :"runtime" LOGIN PASSWORD :'runtime_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
ALTER DATABASE :"DBNAME" OWNER TO :"migrator";
ALTER SCHEMA public OWNER TO :"migrator";
GRANT CONNECT ON DATABASE :"DBNAME" TO :"runtime";
GRANT USAGE ON SCHEMA public TO :"runtime";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"runtime";
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO :"runtime";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"runtime";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator" IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO :"runtime";
SQL
