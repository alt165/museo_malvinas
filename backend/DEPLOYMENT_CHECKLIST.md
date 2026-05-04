# Backend Deployment Checklist

## Required Environment Variables

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KEYCLOAK_ISSUER_URI`
- `KEYCLOAK_JWK_SET_URI`
- `KEYCLOAK_CLIENT_ID`
- `APP_CORS_ALLOWED_ORIGINS`

## Production Checks

- Use managed PostgreSQL or a PostgreSQL container with non-default credentials and persistent backups.
- Run Keycloak with `start`, HTTPS, strict hostname validation, non-default admin credentials, and a production database.
- Keep Swagger disabled in `prod`; expose it only in `dev` or behind authenticated internal access.
- Expose only `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, and `/actuator/info`.
- Verify Docker health with `docker compose ps` and `curl /actuator/health/readiness`.
- Store secrets in the deployment secret manager, not in git or plain compose files.
- Send logs to the platform collector and avoid logging JWTs, passwords, cookies, or authorization headers.
- Configure allowed origins explicitly in `APP_CORS_ALLOWED_ORIGINS`; do not use wildcard origins in production.
- Evaluate `no-new-privileges` and dropped Linux capabilities with the final runtime image; the current local Eclipse Temurin image does not start with those flags.
- Confirm backups, restore procedure, TLS termination, ingress limits, and image scanning before release.
