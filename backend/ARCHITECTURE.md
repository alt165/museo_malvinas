# Architecture

## Vision general

El backend sigue una arquitectura en capas:

```text
Controller -> Service -> Repository -> Database
```

Cada capa tiene una responsabilidad concreta:

- `controller`: recibe HTTP, valida DTOs y devuelve respuestas.
- `service`: concentra reglas de negocio y orquestacion.
- `repository`: acceso a datos con Spring Data JPA.
- `entity`: modelo persistente JPA.
- `dto`: contratos externos de entrada y salida.
- `mapper`: conversion entre entidades y DTOs.
- `security`: configuracion de autenticacion/autorizacion con Keycloak.
- `exception`: manejo global de errores.
- `config`: OpenAPI y trazabilidad de requests.

## Flujo request -> response

Flujo tipico para `POST /api/objetos`:

```text
Cliente HTTP
  -> ObjetoMuseoController
  -> validacion de ObjetoMuseoRequestDTO
  -> ObjetoMuseoService
  -> ObjetoMuseoRepository
  -> PostgreSQL
  -> ObjetoMuseoMapper
  -> ObjetoMuseoResponseDTO
  -> HTTP 201
```

Si ocurre una excepcion de negocio o validacion, `GlobalExceptionHandler` transforma el error en `ApiErrorResponse`.

## Decisiones tecnicas

### Spring Boot

Spring Boot provee el runtime principal, inyeccion de dependencias, configuracion por perfiles y auto-configuracion de Web, JPA, Validation, Security y Actuator.

### DTOs y mappers

Los controllers no exponen entidades JPA. Toda entrada y salida se hace mediante records DTO:

- `*RequestDTO`: payloads de entrada con validaciones.
- `*ResponseDTO`: payloads de salida.

Los mappers encapsulan la conversion para evitar logica de serializacion en controllers o repositories.

### JPA y PostgreSQL

La persistencia usa Spring Data JPA con PostgreSQL. Las entidades extienden `EntidadBase`, que modela borrado logico mediante:

- `activo`
- `eliminado`
- `fechaEliminacion`

La aplicacion usa `spring.jpa.open-in-view=false`, por lo que los services deben cargar y mapear lo necesario dentro del limite transaccional correspondiente.

### Flyway

Flyway aplica migraciones desde:

```text
src/main/resources/db/migration
```

Migraciones actuales:

- `V1__initial_schema.sql`: crea schema, tablas, constraints e indices.
- `V2__seed_initial_data.sql`: carga datos iniciales.

En tests de integracion se valida que ambas migraciones corran contra PostgreSQL real mediante Testcontainers.

### Keycloak

El backend es un Resource Server. No autentica usuarios por cuenta propia:

- Recibe JWT Bearer.
- Valida issuer y JWK set.
- Extrae roles de `realm_access.roles` y de `resource_access[museo-backend].roles`.
- Convierte roles a authorities `ROLE_*`.

### Docker

El entorno local usa `docker-compose.yml` con:

- PostgreSQL 16.
- Keycloak 25 con realm local importado.
- Backend construido desde `backend/Dockerfile`.

El backend corre como usuario no root (`spring:spring`) y el compose define healthcheck sobre `/actuator/health/readiness`.

### Observabilidad

La aplicacion usa logs estructurados en consola. `RequestTracingFilter` agrega:

- `requestId`
- usuario autenticado o `anonymous`
- metodo HTTP
- endpoint
- status HTTP

El `requestId` tambien se devuelve en el header `X-Request-Id`.

### Actuator

Solo se exponen endpoints operativos minimos:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/info`

No se exponen metricas sensibles por defecto.

