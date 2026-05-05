# Museo Malvinas Backend

Backend REST para la gestion del inventario patrimonial del museo. El sistema permite registrar objetos, ubicaciones, inventarios, movimientos, exhibiciones, devoluciones, depositantes, veteranos y relaciones entre objetos.

La API esta construida con una arquitectura en capas y usa PostgreSQL como base de datos, Flyway para versionado de schema, Keycloak para autenticacion/autorizacion y Docker Compose para levantar el entorno local completo.

## Stack tecnologico

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security OAuth2 Resource Server
- Spring Boot Actuator
- PostgreSQL 16
- Flyway
- Keycloak 25
- Springdoc OpenAPI / Swagger UI
- Lombok
- Maven
- Docker / Docker Compose
- Testcontainers para tests de integracion con PostgreSQL real

## Levantar el proyecto con Docker

Desde la raiz del repositorio:

```bash
docker compose up --build
```

Puertos por defecto:

- Backend: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- PostgreSQL: `localhost:5432`

Si esos puertos estan ocupados:

```bash
POSTGRES_PORT=55432 BACKEND_PORT=18080 docker compose up --build
```

El compose levanta:

- `museo-postgres`: base PostgreSQL con volumen persistente.
- `museo-keycloak`: Keycloak local con realm importado desde `docker/keycloak/museo-realm.json`.
- `museo-backend`: aplicacion Spring Boot.

## Swagger y documentacion interactiva

En perfil local/dev, Swagger queda disponible en:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

En perfil `prod`, Swagger se deshabilita desde `application-prod.yml`.

## Healthchecks

Actuator expone solo:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/info`

Ejemplo:

```bash
curl http://localhost:8080/actuator/health/readiness
```

Respuesta esperada:

```json
{"status":"UP"}
```

## Documentacion del backend

- [ARCHITECTURE.md](ARCHITECTURE.md): arquitectura, flujo request-response y decisiones tecnicas.
- [DOMAIN.md](DOMAIN.md): entidades, relaciones y reglas de negocio.
- [API.md](API.md): endpoints principales, ejemplos y errores.
- [SECURITY.md](SECURITY.md): Keycloak, roles, permisos y tokens.
- [SETUP.md](SETUP.md): setup local, variables y tests.
- [DEPLOYMENT.md](DEPLOYMENT.md): preparacion para produccion.
- [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md): checklist operativo resumido.

## Tests

Desde `backend/`:

```bash
mvn test
```

La suite incluye tests unitarios de servicios y tests de integracion con Testcontainers y PostgreSQL real. Los tests de integracion validan Flyway, JPA, repositorios, servicios y reglas criticas contra una base real.

