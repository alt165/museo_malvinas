#!/usr/bin/env bash
set -euo pipefail

KCADM=/opt/keycloak/bin/kcadm.sh
SERVER_URL=http://127.0.0.1:8080
REALM=${MUSEO_REALM:?MUSEO_REALM is required}
FRONTEND_CLIENT=${MUSEO_FRONTEND_CLIENT_ID:?MUSEO_FRONTEND_CLIENT_ID is required}
BACKEND_CLIENT=${MUSEO_BACKEND_CLIENT_ID:?MUSEO_BACKEND_CLIENT_ID is required}
ADMIN_CLIENT=${MUSEO_ADMIN_CLIENT_ID:?MUSEO_ADMIN_CLIENT_ID is required}
ADMIN_SECRET=${MUSEO_ADMIN_CLIENT_SECRET:?MUSEO_ADMIN_CLIENT_SECRET is required}
APP_URL=${MUSEO_PUBLIC_APP_URL:?MUSEO_PUBLIC_APP_URL is required}

client_uuid() {
  "$KCADM" get clients -r "$REALM" -q clientId="$1" --fields id --format csv --noquotes | tail -n 1
}

ensure_role() {
  if ! "$KCADM" get roles/"$1" -r "$REALM" >/dev/null 2>&1; then
    "$KCADM" create roles -r "$REALM" -s name="$1" >/dev/null
  fi
}

"$KCADM" config credentials \
  --server "$SERVER_URL" \
  --realm master \
  --user "${MUSEO_BOOTSTRAP_ADMIN_USERNAME:?MUSEO_BOOTSTRAP_ADMIN_USERNAME is required}" \
  --password "${MUSEO_BOOTSTRAP_ADMIN_PASSWORD:?MUSEO_BOOTSTRAP_ADMIN_PASSWORD is required}" >/dev/null

if ! "$KCADM" get realms/"$REALM" >/dev/null 2>&1; then
  "$KCADM" create realms \
    -s realm="$REALM" \
    -s enabled=true \
    -s sslRequired=external \
    -s registrationAllowed=false \
    -s resetPasswordAllowed=true \
    -s bruteForceProtected=true >/dev/null
fi

"$KCADM" update realms/"$REALM" \
  -s enabled=true \
  -s sslRequired=external \
  -s registrationAllowed=false \
  -s resetPasswordAllowed=true \
  -s bruteForceProtected=true \
  -s loginTheme=museo-login >/dev/null

ensure_role ADMIN
ensure_role MUSEOLOGO
ensure_role VIEWER

backend_uuid=$(client_uuid "$BACKEND_CLIENT")
if [[ -z "$backend_uuid" ]]; then
  "$KCADM" create clients -r "$REALM" \
    -s clientId="$BACKEND_CLIENT" \
    -s enabled=true \
    -s publicClient=false \
    -s bearerOnly=true \
    -s protocol=openid-connect >/dev/null
  backend_uuid=$(client_uuid "$BACKEND_CLIENT")
else
  "$KCADM" update clients/"$backend_uuid" -r "$REALM" \
    -s enabled=true -s publicClient=false -s bearerOnly=true -s protocol=openid-connect >/dev/null
fi

frontend_uuid=$(client_uuid "$FRONTEND_CLIENT")
if [[ -z "$frontend_uuid" ]]; then
  "$KCADM" create clients -r "$REALM" \
    -s clientId="$FRONTEND_CLIENT" \
    -s enabled=true \
    -s publicClient=true \
    -s standardFlowEnabled=true \
    -s directAccessGrantsEnabled=false \
    -s implicitFlowEnabled=false \
    -s serviceAccountsEnabled=false \
    -s protocol=openid-connect >/dev/null
  frontend_uuid=$(client_uuid "$FRONTEND_CLIENT")
fi
"$KCADM" update clients/"$frontend_uuid" -r "$REALM" \
  -s enabled=true \
  -s publicClient=true \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=false \
  -s implicitFlowEnabled=false \
  -s serviceAccountsEnabled=false \
  -s rootUrl="$APP_URL" \
  -s baseUrl="$APP_URL" \
  -s adminUrl="$APP_URL" \
  -s 'attributes."pkce.code.challenge.method"=S256' \
  -s "redirectUris=[\"$APP_URL/*\"]" \
  -s "webOrigins=[\"$APP_URL\"]" >/dev/null

mapper_id=
while IFS=, read -r existing_id existing_name; do
  if [[ "$existing_name" == "museo-backend-audience" ]]; then
    mapper_id=$existing_id
    break
  fi
done < <("$KCADM" get clients/"$frontend_uuid"/protocol-mappers/models -r "$REALM" \
  --fields id,name --format csv --noquotes)
mapper_json=$(mktemp)
trap 'rm -f "$mapper_json"' EXIT
printf '%s\n' \
  '{' \
  '  "name": "museo-backend-audience",' \
  '  "protocol": "openid-connect",' \
  '  "protocolMapper": "oidc-audience-mapper",' \
  '  "consentRequired": false,' \
  '  "config": {' \
  "    \"included.client.audience\": \"$BACKEND_CLIENT\"," \
  '    "id.token.claim": "false",' \
  '    "access.token.claim": "true",' \
  '    "lightweight.claim": "false"' \
  '  }' \
  '}' > "$mapper_json"
if [[ -z "$mapper_id" ]]; then
  "$KCADM" create clients/"$frontend_uuid"/protocol-mappers/models -r "$REALM" -f "$mapper_json" >/dev/null
else
  "$KCADM" update clients/"$frontend_uuid"/protocol-mappers/models/"$mapper_id" -r "$REALM" -f "$mapper_json" >/dev/null
fi

admin_uuid=$(client_uuid "$ADMIN_CLIENT")
if [[ -z "$admin_uuid" ]]; then
  "$KCADM" create clients -r "$REALM" \
    -s clientId="$ADMIN_CLIENT" \
    -s enabled=true \
    -s publicClient=false \
    -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s protocol=openid-connect \
    -s secret="$ADMIN_SECRET" >/dev/null
  admin_uuid=$(client_uuid "$ADMIN_CLIENT")
else
  "$KCADM" update clients/"$admin_uuid" -r "$REALM" \
    -s enabled=true \
    -s publicClient=false \
    -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s secret="$ADMIN_SECRET" >/dev/null
fi

"$KCADM" add-roles -r "$REALM" \
  --uusername "service-account-$ADMIN_CLIENT" \
  --cclientid realm-management \
  --rolename manage-users \
  --rolename view-users \
  --rolename query-users \
  --rolename view-realm >/dev/null

echo "Realm $REALM configured without application users or development passwords."
