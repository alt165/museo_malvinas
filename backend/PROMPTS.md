# Prompts de Desarrollo del Backend

Este documento registra los prompts que guiaron el desarrollo y preparacion del backend. Sirve como bitacora tecnica para nuevos desarrolladores y como referencia para reproducir decisiones de implementacion con agentes de IA.

Las etapas respetan las guias del proyecto:

- `AGENTS.md`: reglas de arquitectura, seguridad, DTOs, mappers y flujo de desarrollo.
- `architecture.md`: arquitectura en capas y decisiones de dominio.
- `coding-standards.md`: convenciones de codigo, testing y buenas practicas.
- Estructura actual del proyecto bajo `com.proveedores`.

## 1. Base arquitectonica del backend

### Prompt utilizado

```text
Usar como referencia:
- agents.md
- architecture.md
- coding-standards.md

Construir el backend respetando arquitectura en capas:
controller -> service -> repository -> database.

Usar Java 17, Spring Boot, Spring Web, Spring Data JPA, PostgreSQL, Docker, Keycloak y Lombok.

Implementar entidades, repositorios, DTOs, mappers, servicios, controllers, seguridad y tests.

No exponer entidades directamente.
Usar DTOs para entrada/salida.
Usar borrado logico con activo, eliminado y fechaEliminacion.
No persistir roles locales; Keycloak es la fuente de verdad.
```

### Objetivo del prompt

Definir la base del backend y alinear la implementacion con la arquitectura esperada del proyecto. Esta etapa establece paquetes, responsabilidades por capa, modelo de dominio, persistencia, API REST y seguridad.

### Resultado esperado

- Proyecto Spring Boot organizado bajo `com.proveedores`.
- Paquetes `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`, `security` y `exception`.
- Entidades JPA con relaciones reales y borrado logico.
- DTOs para requests y responses.
- Mappers para separar conversion Entity/DTO.
- Services con reglas de negocio.
- Controllers REST sin logica de negocio.
- Repositories Spring Data JPA.
- Seguridad con JWT emitido por Keycloak.
- Dockerfile y Docker Compose para entorno local.

## 2. Modelado de dominio patrimonial

### Prompt utilizado

```text
Modelar el dominio del sistema de inventario del museo.

Incluir:
- objetos del museo
- objetos digitales
- categorias
- inventario
- movimientos de inventario
- ubicaciones
- exhibiciones
- objetos en exhibicion
- veteranos
- actuaciones de veteranos
- depositantes
- relaciones entre objetos
- auditoria
- usuarios como referencia local a Keycloak

Respetar:
- ObjetoDigital hereda de ObjetoMuseo con estrategia JOINED.
- Usar Exhibicion como unico nombre para muestras del museo.
- Evitar @ManyToMany directo; usar entidades intermedias.
- No persistir roles locales.
```

### Objetivo del prompt

Construir un modelo de datos consistente con el dominio del museo y preparado para evolucionar sin perder control sobre relaciones y atributos propios de cada vinculo.

### Resultado esperado

- `ObjetoMuseo` como entidad central.
- `ObjetoDigital` como subtipo de `ObjetoMuseo`.
- Entidades intermedias: `ObjetoCategoria`, `ObjetoDepositante`, `ObjetoVeterano`, `ExhibicionObjeto` y `RelacionObjeto`.
- `Inventario` asociado a objeto y ubicacion.
- `MovimientoInventario` para historial operativo.
- `Exhibicion` y `ExhibicionObjeto` para gestion de muestras y devoluciones.
- `Veterano` y `ActuacionVeterano` para contexto historico.
- `Usuario` solo como referencia a Keycloak.

## 3. Migraciones Flyway y datos iniciales

### Prompt utilizado

```text
Agregar migraciones Flyway para PostgreSQL.

Crear:
- V1__initial_schema.sql
- V2__seed_initial_data.sql

Validar tablas, claves foraneas, indices y datos iniciales.
Mantener schema compatible con entidades JPA.
```

### Objetivo del prompt

Versionar la base de datos y evitar que el schema dependa de generacion automatica de Hibernate. Flyway debe ser la fuente de verdad para la estructura de PostgreSQL.

### Resultado esperado

- Migraciones en `src/main/resources/db/migration`.
- Schema inicial con tablas, constraints e indices.
- Datos iniciales para operar localmente.
- `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway habilitado por defecto.

## 4. Seguridad con Keycloak

### Prompt utilizado

```text
Implementar seguridad con Keycloak.

Requerimientos:
- Spring Security como OAuth2 Resource Server.
- Validar JWT.
- Extraer roles desde realm_access y resource_access.
- Proteger /api/** segun roles.
- Mantener publicos healthchecks y Swagger local.
- No implementar autenticacion propia.
- No persistir roles locales.
```

### Objetivo del prompt

Integrar autenticacion/autorizacion externa mediante Keycloak y mantener el backend como resource server stateless.

### Resultado esperado

- `SecurityConfig` con sesiones stateless.
- Validacion JWT usando issuer y JWK set.
- `KeycloakJwtAuthenticationConverter` para roles.
- Roles convertidos a authorities `ROLE_*`.
- Permisos:
  - `GET /api/**`: `ADMIN`, `OPERATOR`, `VIEWER`.
  - escrituras en `/api/**`: `ADMIN`, `OPERATOR`.
- Realm local en `docker/keycloak/museo-realm.json`.

## 5. Tests de integracion con Testcontainers y PostgreSQL

### Prompt utilizado

```text
Agregá tests de integración usando Testcontainers con PostgreSQL.

Usar como referencia:
- agents.md
- architecture.md
- coding-standards.md

Objetivo:
validar que Flyway, JPA, repositorios, services y reglas críticas funcionen contra PostgreSQL real.

Requerimientos:

1. Agregar dependencias necesarias:
   - testcontainers
   - postgresql testcontainer
   - spring-boot-testcontainers si corresponde

2. Crear configuración base para tests de integración.

3. Validar que Flyway aplique migraciones V1 y V2 en el contenedor.

4. Crear tests de integración para:
   - ObjetoMuseoService
   - InventarioService
   - ExhibicionService
   - ExhibicionObjetoService

5. Cubrir:
   - crear objeto real en DB
   - validar número de inventario duplicado
   - crear inventario y registrar movimiento
   - impedir objeto en más de una exhibición activa
   - verificar devolución
   - finalizar exhibición correctamente

6. No usar mocks en estos tests.
7. Mantener los tests unitarios actuales.
8. No modificar lógica salvo bug real detectado.

Al finalizar:
- Ejecutar mvn test
- Informar tests creados
- Informar si Flyway corrió correctamente dentro de Testcontainers
```

### Objetivo del prompt

Asegurar que las reglas de negocio y la persistencia funcionen contra PostgreSQL real, no solo con mocks o bases embebidas.

### Resultado esperado

- Dependencias Testcontainers en `pom.xml`.
- Base comun `IntegrationTestBase`.
- Tests de integracion para servicios criticos.
- Validacion de migraciones Flyway `V1` y `V2`.
- Cobertura de duplicado de inventario, movimientos, exhibiciones activas, devolucion y finalizacion.
- `mvn test` pasando con unitarios e integracion.

## 6. Logging estructurado y trazabilidad de requests

### Prompt utilizado

```text
Implementá logging estructurado y trazabilidad de requests en el backend.

Usar como referencia:
- agents.md
- architecture.md
- coding-standards.md

Objetivo:
mejorar la observabilidad del sistema para debugging y producción.

Requerimientos:

1. Configurar logging en application.yml:
   - nivel INFO por defecto
   - DEBUG para paquetes internos si se requiere

2. Implementar logs en servicios:
   - creación de entidades
   - cambios de estado
   - errores de negocio
   - eventos importantes (ej: finalización de exhibición)

3. Agregar un filtro o interceptor que:
   - genere un requestId único por request
   - lo agregue al MDC (Mapped Diagnostic Context)
   - lo incluya en todos los logs

4. Incluir en logs:
   - requestId
   - usuario (si está autenticado)
   - endpoint
   - método HTTP

5. Configurar formato de logs consistente (JSON o estructurado si es posible).

6. No loggear:
   - tokens JWT
   - datos sensibles

7. No modificar lógica de negocio.

8. Agregar logs en GlobalExceptionHandler para errores.

Al finalizar:
- Ejecutar mvn test
- Mostrar ejemplos de logs generados
- Indicar cómo se ve un flujo completo en logs
```

### Objetivo del prompt

Mejorar observabilidad sin modificar reglas de negocio, incorporando trazabilidad por request y eventos de dominio relevantes.

### Resultado esperado

- Logging configurado en `application.yml`.
- `RequestTracingFilter` con `X-Request-Id` y MDC.
- Logs de creacion, actualizacion, baja logica, cambios de estado y errores de negocio.
- Logs de excepciones centralizados en `GlobalExceptionHandler`.
- Ausencia de tokens JWT y credenciales en logs.
- Tests existentes pasando.

## 7. Actuator y healthchecks de produccion

### Prompt utilizado

```text
Implementá Spring Boot Actuator y healthchecks de producción.

Usar como referencia:
- agents.md
- architecture.md
- coding-standards.md

Objetivo:
exponer endpoints de salud, readiness y liveness para Docker y futuro despliegue.

Requerimientos:

1. Agregar dependencia spring-boot-starter-actuator si no existe.

2. Configurar application.yml para exponer solo:
   - /actuator/health
   - /actuator/info
   - readiness/liveness probes

3. Configurar health groups:
   - liveness
   - readiness

4. Incluir healthcheck del backend en docker-compose.yml usando /actuator/health/readiness.

5. Mantener público sin autenticación:
   - /actuator/health/**
   - /actuator/info

6. No exponer métricas sensibles por ahora.

7. No modificar lógica de negocio.

8. Ejecutar:
   - mvn test
   - docker compose up --build

Al finalizar:
- Informar endpoints Actuator disponibles
- Informar cambios en Docker
- Informar cómo verificar healthcheck
```

### Objetivo del prompt

Preparar endpoints operativos minimos para Docker y despliegues futuros, sin exponer metricas sensibles.

### Resultado esperado

- Dependencia `spring-boot-starter-actuator`.
- Exposicion limitada a `health` e `info`.
- Health groups `liveness` y `readiness`.
- Readiness incluyendo estado de DB.
- Healthcheck de Docker Compose contra `/actuator/health/readiness`.
- Endpoints de health/info publicos.
- `mvn test` y compose funcionando.

## 8. Hardening final para despliegue

### Prompt utilizado

```text
Hacé una revisión final de hardening del backend para preparación de despliegue.

Revisar:
- variables de entorno obligatorias
- perfiles dev/prod
- configuración segura de Docker
- CORS
- errores expuestos al cliente
- logs sin datos sensibles
- Swagger solo para dev o protegido
- configuración de Keycloak para producción
- checklist de despliegue

No modificar lógica de negocio.
Proponer cambios concretos y aplicar solo los seguros.
Ejecutar mvn test y docker compose up --build --wait.
```

### Objetivo del prompt

Revisar la preparacion productiva del backend y aplicar cambios seguros de configuracion, sin afectar comportamiento de dominio.

### Resultado esperado

- `application-prod.yml` con variables obligatorias sin defaults.
- Swagger deshabilitado en `prod`.
- CORS configurable por `APP_CORS_ALLOWED_ORIGINS`.
- Errores Spring sin stacktrace ni detalles sensibles.
- Logs sin mensajes potencialmente sensibles de excepciones de negocio.
- Docker Compose parametrizado por variables.
- Backend con filesystem read-only y `/tmp` como tmpfs.
- Checklist de despliegue documentado.
- `mvn test` pasando.
- `docker compose up --build --wait` con backend healthy.

## 9. Commit del hardening final

### Prompt utilizado

```text
Revisá el árbol de Git, mostrámelo resumido y prepará un commit para los cambios de hardening final.

Requerimientos:
1. Ejecutar git status.
2. Revisar archivos modificados.
3. Confirmar que no haya secretos ni credenciales reales.
4. Ejecutar mvn test.
5. Si todo está correcto, crear commit con el mensaje:
   Harden backend production configuration
6. No hacer push todavía.
```

### Objetivo del prompt

Controlar la calidad del cambio antes de versionarlo, verificando estado del arbol, secretos y tests.

### Resultado esperado

- Resumen de archivos modificados.
- Confirmacion de ausencia de secretos reales.
- `mvn test` exitoso.
- Commit local con mensaje exacto.
- Sin push automatico.

## 10. Documentacion para desarrolladores

### Prompt utilizado

```text
Generá documentación completa del backend para que nuevos desarrolladores puedan entender el sistema.

Usar como referencia:
- agents.md
- architecture.md
- coding-standards.md
- estructura real del proyecto
- entidades, servicios, controllers, seguridad, docker, flyway

Objetivo:
crear documentación clara, profesional y útil (no genérica).

Requerimientos:

1. Crear los siguientes archivos en la raíz del backend:

   - README.md
   - ARCHITECTURE.md
   - DOMAIN.md
   - API.md
   - SECURITY.md
   - SETUP.md
   - DEPLOYMENT.md

2. README.md debe incluir:
   - descripción del sistema
   - stack tecnológico
   - cómo levantar el proyecto con Docker
   - links a Swagger
   - links a otros documentos

3. ARCHITECTURE.md:
   - arquitectura en capas
   - flujo request → response
   - decisiones técnicas (Spring, Keycloak, Flyway, Docker)

4. DOMAIN.md:
   - entidades principales
   - relaciones
   - reglas de negocio importantes
   - conceptos como inventario, exhibición, veteranos

5. API.md:
   - descripción de endpoints principales
   - ejemplos de uso
   - explicación de respuestas y errores

6. SECURITY.md:
   - cómo funciona Keycloak
   - cómo obtener token
   - roles y permisos
   - cómo autenticarse

7. SETUP.md:
   - pasos para levantar el proyecto
   - variables necesarias
   - uso de docker compose
   - cómo correr tests

8. DEPLOYMENT.md:
   - configuración para producción
   - variables obligatorias
   - recomendaciones de seguridad
   - checklist de deploy

9. No inventar cosas que no existan.
10. Usar lenguaje claro y profesional.
11. Evitar emojis.
12. Incluir ejemplos concretos.

Al finalizar:
- mostrar estructura de archivos generados
- mostrar ejemplos de contenido
```

### Objetivo del prompt

Crear documentacion operativa y tecnica para que nuevos desarrolladores entiendan la arquitectura, dominio, API, seguridad, setup y despliegue del backend.

### Resultado esperado

- Siete documentos en la raiz de `backend`.
- Documentacion basada en estructura real del proyecto.
- Ejemplos concretos de Docker, Swagger, healthchecks, tokens y endpoints.
- Descripcion de entidades, servicios, controllers, seguridad, Docker y Flyway.
- Sin inventar funcionalidades no implementadas.

## 11. Versionado y publicacion de documentacion

### Prompt utilizado

```text
hace commit y push
```

### Objetivo del prompt

Versionar y publicar los documentos generados en el repositorio remoto.

### Resultado esperado

- Commit con documentacion del backend.
- Push a `origin/main`.
- Arbol Git limpio despues del push.

## 12. Documentacion del proceso de prompts

### Prompt utilizado

```text
Generá un archivo PROMPTS.md que documente todo el proceso de desarrollo del backend basado en los prompts utilizados.

Debe incluir:
- cada etapa del desarrollo
- el prompt utilizado
- el objetivo del prompt
- el resultado esperado

Usar como referencia:
- agents.md
- architecture.md
- estructura actual del proyecto

Formato claro y ordenado.
```

### Objetivo del prompt

Registrar el proceso de desarrollo guiado por prompts para que el equipo pueda auditar decisiones, repetir etapas y entender como se llego a la estructura actual.

### Resultado esperado

- `PROMPTS.md` en la raiz de `backend`.
- Etapas ordenadas cronologicamente.
- Prompt, objetivo y resultado esperado por etapa.
- Referencia explicita a arquitectura, dominio, seguridad, testing, observabilidad, despliegue y documentacion.

