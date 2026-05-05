# Deployment

## Objetivo

Esta guia resume la configuracion necesaria para preparar el backend para produccion. No reemplaza la configuracion propia del proveedor de infraestructura, pero define los puntos que deben estar resueltos antes de publicar el servicio.

## Perfil de produccion

Activar:

```text
SPRING_PROFILES_ACTIVE=prod
```

El archivo `application-prod.yml` exige variables sin defaults para evitar arranques accidentales con valores locales.

## Variables obligatorias

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Activa configuracion productiva. |
| `SPRING_DATASOURCE_URL` | JDBC URL de PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Password de base de datos. |
| `KEYCLOAK_ISSUER_URI` | Issuer publico esperado del realm. |
| `KEYCLOAK_JWK_SET_URI` | Endpoint de claves publicas para validar JWT. |
| `KEYCLOAK_CLIENT_ID` | Cliente usado para extraer roles de `resource_access`. |
| `APP_CORS_ALLOWED_ORIGINS` | Lista explicita de origins permitidos. |

Ejemplo:

```bash
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://db.example.internal:5432/museo \
SPRING_DATASOURCE_USERNAME=museo_app \
SPRING_DATASOURCE_PASSWORD='<secret>' \
KEYCLOAK_ISSUER_URI=https://auth.example.com/realms/museo \
KEYCLOAK_JWK_SET_URI=https://auth.example.com/realms/museo/protocol/openid-connect/certs \
KEYCLOAK_CLIENT_ID=museo-backend \
APP_CORS_ALLOWED_ORIGINS=https://app.example.com \
java -jar app.jar
```

No guardar secretos reales en git.

## Base de datos

Recomendaciones:

- Usar PostgreSQL administrado o un contenedor con backups y monitoreo.
- Usar credenciales no compartidas y rotables.
- Mantener `spring.jpa.hibernate.ddl-auto=validate`.
- Mantener Flyway habilitado.
- Ejecutar backups y probar restore antes del primer deploy productivo.

## Flyway

Las migraciones viven en:

```text
src/main/resources/db/migration
```

En produccion, Flyway debe aplicar migraciones versionadas antes de que la aplicacion quede lista. El readiness incluye `db`, por lo que si la base no esta disponible el healthcheck debe fallar.

## Keycloak en produccion

El `docker-compose.yml` local usa `start-dev` y credenciales de desarrollo. Para produccion:

- Usar `kc.sh start`, no `start-dev`.
- Habilitar HTTPS.
- Activar hostname strict.
- Usar credenciales reales de administracion.
- Usar una base de datos productiva para Keycloak.
- Revisar redirect URIs y web origins.
- Eliminar usuarios locales de ejemplo.
- Confirmar roles reales requeridos por el backend: `ADMIN`, `OPERATOR`, `VIEWER`.

## Swagger

En perfil `prod`, Swagger esta deshabilitado:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

Si se requiere documentacion interactiva en entornos internos, protegerla con autenticacion o exponerla solo en redes internas.

## CORS

Configurar `APP_CORS_ALLOWED_ORIGINS` con origins concretos:

```text
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
```

No usar wildcard en produccion.

## Docker

El backend se construye con `backend/Dockerfile`.

Medidas actuales:

- Runtime separado del build.
- Usuario no root (`spring:spring`).
- Healthcheck en compose contra `/actuator/health/readiness`.
- Filesystem read-only y `/tmp` como `tmpfs` en compose local.

Pendiente para imagen final:

- Evaluar `no-new-privileges` y drop de capabilities con la imagen runtime definitiva. La imagen local de Eclipse Temurin no arranco con esas opciones en las pruebas realizadas.

## Healthchecks

Endpoints disponibles:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/info`

Verificacion:

```bash
curl -fsS http://localhost:8080/actuator/health/readiness
```

## Logs

Los logs son estructurados en consola e incluyen:

- timestamp
- level
- logger
- thread
- requestId
- user
- method
- endpoint
- status
- message

No se deben loguear tokens JWT, passwords, cookies ni headers de autorizacion.

## Checklist de deploy

- Definir `SPRING_PROFILES_ACTIVE=prod`.
- Configurar todas las variables obligatorias.
- Usar secretos gestionados por la plataforma.
- Confirmar conectividad con PostgreSQL.
- Confirmar migraciones Flyway.
- Confirmar realm, issuer y JWK set de Keycloak.
- Validar roles `ADMIN`, `OPERATOR`, `VIEWER`.
- Configurar CORS con origins reales.
- Confirmar Swagger deshabilitado o protegido.
- Verificar `/actuator/health/readiness`.
- Configurar backups y restore.
- Configurar recoleccion de logs.
- Ejecutar `mvn test` y pruebas de smoke antes de promover.

