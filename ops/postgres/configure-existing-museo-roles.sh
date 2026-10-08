#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$REPO_DIR"
# shellcheck source=ops/lib.sh
source ops/lib.sh
load_production_env

: "${MUSEO_DB_LEGACY_OWNER:?Set MUSEO_DB_LEGACY_OWNER to the role that owns the existing V28 schema}"
: "${MUSEO_DB_LEGACY_ADMIN_USER:?Set MUSEO_DB_LEGACY_ADMIN_USER to a current PostgreSQL superuser for this one-time migration}"

for role in "$MUSEO_DB_LEGACY_OWNER" "$MUSEO_DB_LEGACY_ADMIN_USER" "$MUSEO_DB_ADMIN_USER" "$MUSEO_DB_MIGRATOR_USER" "$MUSEO_DB_RUNTIME_USER"; do
  [[ "$role" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || { echo "Invalid PostgreSQL role name" >&2; exit 1; }
done

[[ "$MUSEO_DB_LEGACY_OWNER" != "$MUSEO_DB_MIGRATOR_USER" ]] || {
  echo "Legacy owner and migrator must be different roles" >&2
  exit 1
}

compose exec -T postgres-museo psql --set=ON_ERROR_STOP=1 \
  --username "$MUSEO_DB_LEGACY_ADMIN_USER" --dbname "$MUSEO_DB_NAME" \
  --set=bootstrap="$MUSEO_DB_ADMIN_USER" \
  --set=bootstrap_password="$MUSEO_DB_ADMIN_PASSWORD" \
  --set=legacy_owner="$MUSEO_DB_LEGACY_OWNER" \
  --set=migrator="$MUSEO_DB_MIGRATOR_USER" \
  --set=migrator_password="$MUSEO_DB_MIGRATOR_PASSWORD" \
  --set=runtime="$MUSEO_DB_RUNTIME_USER" \
  --set=runtime_password="$MUSEO_DB_RUNTIME_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L SUPERUSER CREATEDB CREATEROLE INHERIT', :'bootstrap', :'bootstrap_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'bootstrap') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L SUPERUSER CREATEDB CREATEROLE INHERIT', :'bootstrap', :'bootstrap_password') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS', :'migrator', :'migrator_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'migrator') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS', :'migrator', :'migrator_password') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS', :'runtime', :'runtime_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'runtime') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS', :'runtime', :'runtime_password') \gexec
ALTER DATABASE :"DBNAME" OWNER TO :"migrator";
ALTER SCHEMA public OWNER TO :"migrator";
SELECT format('REASSIGN OWNED BY %I TO %I', :'legacy_owner', :'migrator') \gexec
SELECT format('ALTER ROLE %I NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS', :'runtime') \gexec
GRANT CONNECT ON DATABASE :"DBNAME" TO :"runtime";
GRANT USAGE ON SCHEMA public TO :"runtime";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"runtime";
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO :"runtime";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator" IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"runtime";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator" IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO :"runtime";
SQL

compose exec -T postgres-museo psql --tuples-only --no-align \
  --username "$MUSEO_DB_ADMIN_USER" --dbname "$MUSEO_DB_NAME" \
  --command "SELECT rolname || ':' || rolsuper || ':' || rolcreatedb || ':' || rolcreaterole || ':' || rolbypassrls FROM pg_roles WHERE rolname IN ('$MUSEO_DB_MIGRATOR_USER', '$MUSEO_DB_RUNTIME_USER') ORDER BY rolname"
