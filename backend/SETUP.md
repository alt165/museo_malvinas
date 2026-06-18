# Setup

## Requisitos

- Java 17
- Maven 3.9 o compatible
- Docker
- Docker Compose

## Levantar entorno completo con Docker

Desde la raiz del repositorio:

```bash
docker compose up --build
```

Servicios:

- PostgreSQL: `localhost:5432`
- Keycloak: `http://localhost:8081`
- Backend: `http://localhost:8080`

Para usar puertos alternativos:

```bash
POSTGRES_PORT=55432 BACKEND_PORT=18080 KEYCLOAK_PORT=18081 FRONTEND_PORT=13000 docker compose up --build --wait
```

## Verificar que el backend esta listo

```bash
curl http://localhost:8080/actuator/health/readiness
```

Respuesta esperada:

```json
{"status":"UP"}
```

## Swagger local

Con el backend levantado:

```text
http://localhost:8080/swagger-ui/index.html
```

## Variables de entorno locales

La aplicacion tiene defaults para desarrollo en `application.yml`:

| Variable | Default local |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/museo` |
| `SPRING_DATASOURCE_USERNAME` | `museo` |
| `SPRING_DATASOURCE_PASSWORD` | `museo` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` |
| `SPRING_FLYWAY_ENABLED` | `true` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8081/realms/museo` |
| `KEYCLOAK_JWK_SET_URI` | `http://localhost:8081/realms/museo/protocol/openid-connect/certs` |
| `KEYCLOAK_CLIENT_ID` | `museo-backend` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |
| `APP_STORAGE_OBJECT_FILES_DIR` | `./storage/object-files` |
| `APP_STORAGE_OBJECT_PHOTOS_DIR` | `./storage/object-photos` |
| `APP_STORAGE_SIGNED_RECEIPTS_DIR` | `./storage/signed-receipts` |
| `APP_UPLOAD_MAX_PHOTO_SIZE_MB` | `5` |
| `APP_UPLOAD_MAX_RECEIPT_SIZE_MB` | `10` |
| `APP_UPLOAD_MAX_SIGNED_RECEIPT_SIZE_MB` | `10` |
| `SERVER_PORT` | `8080` |
| `APP_LOG_LEVEL` | `INFO` |

## Ejecutar backend sin Docker

Primero levantar PostgreSQL y Keycloak, por ejemplo con Docker Compose:

```bash
docker compose up postgres keycloak
```

Luego desde `backend/`:

```bash
mvn spring-boot:run
```

Si PostgreSQL usa puerto alternativo:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/museo mvn spring-boot:run
```

## Obtener token local

```bash
curl -X POST http://localhost:8081/realms/museo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=museo-local" \
  -d "username=admin" \
  -d "password=admin"
```

Usar el `access_token`:

```bash
export TOKEN="<access_token>"
curl http://localhost:8080/api/objetos \
  -H "Authorization: Bearer $TOKEN"
```

## Ejecutar tests

Desde `backend/`:

```bash
mvn test
```

Los tests incluyen:

- Unit tests de services.
- Tests web de controller.
- Tests de integracion con Testcontainers y PostgreSQL real.
- Validacion de migraciones Flyway.

## Storage de archivos

Las fotos de objetos y los recibos escaneados no se guardan como binarios en PostgreSQL. El backend guarda esos archivos debajo de `APP_STORAGE_OBJECT_FILES_DIR`, separados internamente por objeto:

```text
objeto-{id}/fotos/
objeto-{id}/recibos/
```

Las copias firmadas de recibos emitidos siguen usando `APP_STORAGE_SIGNED_RECEIPTS_DIR`. PostgreSQL conserva metadata/rutas internas y los archivos se descargan por endpoints autenticados.

## Reset de datos locales

El estado de PostgreSQL se guarda en el volumen `postgres_data`. Para borrar datos locales:

```bash
docker compose down -v
```

Luego levantar nuevamente:

```bash
docker compose up --build
```
