# Auditoría Pre-Producción — Sistema Museo

**Fecha de auditoría:** 2026-10-05
**Alcance:** repositorio completo, ejecución local de builds y pruebas, inspección del stack Docker activo y pruebas funcionales acotadas de autenticación/autorización y fotografías.
**Regla aplicada:** no se modificó código ni configuración del producto. No se ejecutaron operaciones destructivas ni se alteraron datos de producción.

## Convenciones de evidencia

- **Verificado:** observado mediante ejecución, consulta HTTP, resultado de herramienta o inspección directa inequívoca.
- **Inferido:** conclusión fundada en código/configuración, sin reproducir el escenario completo.
- **No verificable:** faltaron infraestructura, credenciales, procedimiento seguro o un entorno descartable adecuado.

## 1. Resumen ejecutivo

El sistema presenta una base técnica razonable: backend Spring Boot con separación por capas, validación de DTO, transacciones en servicios, Flyway, PostgreSQL, autenticación JWT con Keycloak, manejo centralizado de errores, almacenamiento con nombres aleatorios, generación de versión pública de imágenes, perfil `prod`, Actuator y un frontend Next.js que compila correctamente. La base nueva pudo ejecutar las 28 migraciones Flyway y el stack local estaba operativo.

No obstante, el estado actual contiene bloqueantes de producción:

1. **Exposición comprobada de fotografías originales:** un usuario `OPERATOR` obtiene exactamente los mismos bytes que el endpoint exclusivo de originales para `ADMIN`, utilizando el endpoint común de fotografía.
2. **Suite backend roja:** de 248 pruebas, 4 fallaron y 35 terminaron con error. Varias suites de integración de objetos, inventario, colecciones y exhibiciones no llegan a validar su objetivo porque sus fixtures quedaron incompatibles con reglas nuevas.
3. **Dependencias frontend con vulnerabilidades conocidas:** `npm audit --omit=dev` informó 5 vulnerabilidades de runtime, incluida 1 crítica en la versión instalada de Next.js.
4. **Identidad no apta para despliegue con el Compose actual:** Keycloak se inicia con `start-dev`, credenciales de desarrollo versionadas, sin base persistente propia ni healthcheck. Recrear el contenedor puede perder el estado de identidad y reimportar cuentas conocidas.
5. **No existe una estrategia operativa y probada de backup/restauración** para PostgreSQL, originales, versiones públicas y Keycloak.
6. **Riesgos reales de integridad:** doble reserva concurrente de objetos en exhibiciones, endpoint alternativo que evita la validación temporal completa, historial de inventario mutable y conflictos entre borrado lógico y restricciones únicas.

La compilación sin tests no compensa estos riesgos. El sistema puede continuar hacia una etapa de remediación y nueva validación, pero no debe desplegarse todavía.

## 2. Veredicto

# 🔴 NO APTO PARA PRODUCCIÓN

La decisión se basa en vulnerabilidades y fallos reproducidos, no sólo en riesgos teóricos: acceso indebido a originales, 39 tests backend no exitosos y vulnerabilidades críticas/altas en dependencias de runtime. También faltan garantías operativas esenciales sobre identidad, backups y restauración.

## 3. Hallazgos críticos

### SEC-001 — El endpoint común entrega el original a OPERATOR

**Estado:** verificado funcionalmente y por código.

- La regla HTTP reserva `/api/objetos/*/fotos/*/original` a `ADMIN` ([`SecurityConfig.java:62`](backend/src/main/java/com/proveedores/security/SecurityConfig.java#L62)).
- Sin embargo, `descargar()` decide qué variante servir mediante `puedeVerOriginal()` ([`FotoObjetoMuseoService.java:108`](backend/src/main/java/com/proveedores/service/FotoObjetoMuseoService.java#L108)). Ese método delega en `puedeVerPrivados()`, que acepta `ADMIN`, `OPERATOR` y `MUSEOLOGO` ([`FotoObjetoMuseoService.java:182`](backend/src/main/java/com/proveedores/service/FotoObjetoMuseoService.java#L182), [`FotoObjetoMuseoService.java:206`](backend/src/main/java/com/proveedores/service/FotoObjetoMuseoService.java#L206)).
- Prueba ejecutada sobre el stack local: el endpoint común con `OPERATOR` respondió `200`; el endpoint de original con `ADMIN` respondió `200`; ambos archivos tuvieron 176.416 bytes y `cmp` confirmó identidad byte a byte.
- **Impacto:** incumplimiento directo del requisito de mantener privados/protegidos los originales; exposición de activos de alta resolución a un rol no autorizado.

### TEST-001 — La suite backend no es una barrera de liberación válida

**Estado:** verificado.

- `mvn test`: **248 ejecutados, 209 exitosos, 4 fallidos, 35 errores, 0 ignorados**.
- Las suites afectadas incluyen `ObjetoMuseoServiceIntegrationTest`, `InventarioServiceIntegrationTest`, `ColeccionObjetoServiceIntegrationTest`, `ExhibicionObjetoServiceIntegrationTest` y `ExhibicionServiceIntegrationTest`.
- La causa dominante es que los datos de prueba no satisfacen la validación actual de ficha completa (`ObjetoMuseoService.java:872`), por lo que las pruebas abortan antes de ejercitar las reglas que pretenden verificar.
- **Impacto:** no existe evidencia automatizada confiable para publicar cambios en las áreas centrales del dominio.

### DEP-001 — Vulnerabilidades críticas y altas en dependencias frontend de runtime

**Estado:** verificado mediante `npm audit` con el lockfile vigente.

- `npm audit --omit=dev --json`: **5 vulnerabilidades de runtime: 1 crítica, 3 altas y 1 moderada**.
- Afectan al `next` directo instalado (16.2.5) y a dependencias transitivas como `sharp`, `postcss`, `nanoid` y `baseline-browser-mapping`.
- El informe de npm incluye avisos críticos para el rango instalado de Next.js. No se actualizó ninguna dependencia durante esta auditoría.
- **Impacto:** exposición de la capa web a vulnerabilidades conocidas, incluida una clase crítica informada por la herramienta.

### IAM-001 — El Compose disponible no puede usarse como despliegue de producción

**Estado:** verificado por configuración e inspección del contenedor.

- Keycloak usa `start-dev --import-realm`, HTTP y hostname no estricto ([`docker-compose.yml:21`](docker-compose.yml#L21)).
- El realm versiona un secreto de cliente de desarrollo y tres contraseñas no temporales; los valores fueron deliberadamente redactados en este informe ([`museo-realm.json:68`](backend/docker/keycloak/museo-realm.json#L68), [`museo-realm.json:78`](backend/docker/keycloak/museo-realm.json#L78)).
- No hay volumen/base de datos persistente para Keycloak; sólo se montan el import y el tema ([`docker-compose.yml:34`](docker-compose.yml#L34)).
- El Compose no activa `SPRING_PROFILES_ACTIVE=prod`, por lo que el backend parte de defaults de desarrollo y Swagger queda habilitado.
- **Impacto:** pérdida/recreación del estado de usuarios y roles, cuentas conocidas, configuración insegura de identidad y posibilidad de desplegar accidentalmente el perfil equivocado.

### OPS-001 — No hay backup y restauración implementados y probados

**Estado:** verificado documentalmente; no se encontró automatización ni evidencia de ensayos.

- La documentación sólo recomienda configurar backups y probar la restauración.
- No existen scripts, jobs, política de retención, cifrado, RPO/RTO, monitoreo ni actas de restauración para PostgreSQL, originales, versiones públicas o identidad.
- **Impacto:** una falla de disco, borrado del volumen o error operacional puede producir pérdida irrecuperable del inventario documental y fotográfico.

## 4. Hallazgos de alta prioridad

### IAM-002 — `MUSEOLOGO` no está implementado como rol operativo coherente

**Estado:** verificado.

- Realm, administración de usuarios y frontend sólo reconocen `ADMIN`, `OPERATOR`, `VIEWER` ([`museo-realm.json:5`](backend/docker/keycloak/museo-realm.json#L5), [`KeycloakAdminService.java:34`](backend/src/main/java/com/proveedores/service/KeycloakAdminService.java#L34), [`session.ts:1`](frontend/src/models/session.ts#L1)).
- `MUSEOLOGO` aparece aisladamente en acceso a fotos/campos privados, pero queda fuera del `GET /api/**` general y de toda mutación ([`SecurityConfig.java:63`](backend/src/main/java/com/proveedores/security/SecurityConfig.java#L63), [`SecurityConfig.java:72`](backend/src/main/java/com/proveedores/security/SecurityConfig.java#L72)).
- La aplicación no permite asignarlo desde la administración ([`KeycloakAdminService.java:223`](backend/src/main/java/com/proveedores/service/KeycloakAdminService.java#L223)).
- **Impacto:** el modelo solicitado de permisos no existe realmente; intentar usar ese rol genera accesos parciales e incoherentes.

### EXH-001 — Posible doble reserva concurrente de un objeto

**Estado:** inferido con evidencia de código.

- La disponibilidad se valida consultando relaciones antes de insertar/actualizar, sin bloqueo pesimista, versión optimista ni restricción de exclusión en PostgreSQL.
- Dos transacciones concurrentes pueden observar disponibilidad y confirmar exhibiciones solapadas.
- **Impacto:** un mismo objeto puede quedar comprometido simultáneamente en dos exhibiciones.

### EXH-002 — El endpoint directo de relación evita reglas temporales completas

**Estado:** verificado por inspección.

- `ExhibicionObjetoService` sólo rechaza otra relación cuando la exhibición destino ya está `ACTIVA`; una `PLANIFICADA` retorna sin validar solapamientos ([`ExhibicionObjetoService.java:203`](backend/src/main/java/com/proveedores/service/ExhibicionObjetoService.java#L203)).
- Así, la API de relaciones puede producir estados que el flujo agregado de `ExhibicionService` intenta impedir.
- **Impacto:** reservas inconsistentes mediante manipulación del endpoint, aunque la UI principal no ofrezca ese flujo.

### EXH-003 — Finalizar/cancelar marca devoluciones como verificadas sin verificación humana

**Estado:** verificado por inspección.

- Al finalizar, todas las relaciones pasan a `DEVUELTO`, `devolucionVerificada=true` y fecha de retiro actual ([`ExhibicionService.java:445`](backend/src/main/java/com/proveedores/service/ExhibicionService.java#L445)). Cancelar aplica una transformación equivalente.
- No se registra quién verificó ni una fecha de verificación correspondiente.
- La documentación de API indica que no se debería finalizar con pendientes, pero la implementación los confirma automáticamente.
- **Impacto:** creación de evidencia de devolución falsa o incompleta y pérdida de trazabilidad física.

### DB-001 — Borrado lógico incompatible con varias restricciones `UNIQUE`

**Estado:** verificado por esquema y servicios.

- Persisten restricciones globales sobre categoría, ubicación y relaciones de objeto ([`V1__initial_schema.sql:41`](backend/src/main/resources/db/migration/V1__initial_schema.sql#L41), [`V1__initial_schema.sql:52`](backend/src/main/resources/db/migration/V1__initial_schema.sql#L52), [`V1__initial_schema.sql:133`](backend/src/main/resources/db/migration/V1__initial_schema.sql#L133), [`V1__initial_schema.sql:199`](backend/src/main/resources/db/migration/V1__initial_schema.sql#L199)).
- Tras una baja lógica, volver a agregar una categoría a un objeto, una relación objeto-objeto o un objeto a una exhibición intenta insertar una fila nueva y choca con la fila eliminada.
- El vínculo objeto-veterano sí implementa reactivación; los flujos anteriores no lo hacen de forma consistente.
- DNI/CUIT de depositante conservan unicidad incluso eliminado, pero ese caso está tratado explícitamente con conflicto y restauración. Categorías/ubicaciones no tienen un flujo equivalente documentado.
- **Impacto:** operaciones legítimas de restauración/reasociación devuelven `409` o quedan bloqueadas por datos históricos.

### INV-001 — Historial de inventario mutable e inconsistente

**Estado:** verificado por inspección.

- Al editar inventario puede cambiarse el `objetoMuseo` dueño del registro ([`InventarioService.java:75`](backend/src/main/java/com/proveedores/service/InventarioService.java#L75)), conservando estado/ubicación anteriores y asociando el eventual movimiento al objeto nuevo.
- La API genérica permite crear, editar y dar de baja movimientos históricos, incluso cambiar objeto, usuario, fecha y tipo ([`MovimientoInventarioService.java:37`](backend/src/main/java/com/proveedores/service/MovimientoInventarioService.java#L37), [`MovimientoInventarioService.java:57`](backend/src/main/java/com/proveedores/service/MovimientoInventarioService.java#L57)).
- **Impacto:** la cadena histórica deja de ser evidencia confiable y puede quedar ligada al objeto incorrecto.

### AUD-001 — Auditoría funcional incompleta

**Estado:** verificado por inventario de llamadas a `AuditoriaObjetoService`.

- La auditoría se concentra en cambios asociados a `OBJETO_MUSEO`, exhibiciones, colecciones, embargos y comodatos.
- No se halló auditoría equivalente para administración de usuarios/roles/reset de contraseñas, depositantes y su restauración, categorías, ubicaciones, veteranos/multimedia, inventarios, CRUD directo de movimientos, fotografías/visibilidad, recibos ni configuración.
- La restauración de un objeto (`ObjetoMuseoService.restaurar`) no registra una entrada en el historial de auditoría.
- **Impacto:** acciones sensibles no son atribuibles ni reconstruibles de manera uniforme.

### FILE-001 — Validación y atomicidad desiguales en archivos

**Estado:** verificado por inspección; algunos fallos no fueron inyectados.

- Las fotos de objetos tienen una defensa sólida: MIME permitido, decodificación real, comparación de formato, límite de píxeles, nombre UUID y generación pública con marca de agua.
- Los recibos escaneados e imágenes/multimedia de veteranos confían principalmente en el MIME declarado y tamaño; no validan siempre firma mágica/decodificación. Aceptan un archivo corrupto o camuflado con MIME permitido.
- Algunos servicios escriben el archivo antes de confirmar la transacción de base y no registran limpieza al rollback. Reemplazar recibos firmados no elimina necesariamente el anterior.
- La carga múltiple de fotos procesa archivos como operaciones independientes: un fallo tardío puede dejar los anteriores confirmados.
- **Impacto:** archivos huérfanos, operaciones parcialmente completadas y contenido no realmente correspondiente a su tipo declarado.

### FILE-002 — Límite multipart efectivo no coincide con el límite de negocio

**Estado:** inferido por configuración Spring Boot.

- La aplicación declara 5 MB para fotos, pero no configura `spring.servlet.multipart.max-file-size` ni `max-request-size` ([`application.yml:42`](backend/src/main/resources/application.yml#L42)).
- El límite multipart por defecto de Spring puede rechazar solicitudes antes de llegar a la validación propia. Tampoco existe handler específico para exceso multipart.
- **Impacto:** fotos válidas según la aplicación pueden fallar, potencialmente como `500`, antes de la lógica de negocio.

### INFRA-001 — Healthchecks y recuperación Docker insuficientes

**Estado:** verificado.

- Sólo PostgreSQL y backend tienen healthcheck; Keycloak y frontend no ([`docker-compose.yml:13`](docker-compose.yml#L13), [`docker-compose.yml:72`](docker-compose.yml#L72)).
- Backend depende de Keycloak con `service_started`, no saludable ([`docker-compose.yml:60`](docker-compose.yml#L60)).
- Ningún servicio define política `restart`.
- Readiness del backend comprueba aplicación y DB, no Keycloak ni almacenamiento escribible ([`application.yml:82`](backend/src/main/resources/application.yml#L82)).
- **Impacto:** Docker puede considerar operativo un sistema sin login o frontend sano y no recuperarlo automáticamente.

### SUPPLY-001 — Auditoría de vulnerabilidades backend inconclusa

**Estado:** no verificable con las herramientas disponibles.

- OWASP Dependency-Check 13.0.0 fue ejecutado, pero no pudo obtener datos NVD porque el entorno no tenía API key/dataset y terminó con `NoDataException`.
- **Impacto:** no puede afirmarse ausencia de CVE backend antes del despliegue.

## 5. Hallazgos de prioridad media

### PERF-001 — Listados sin paginación y posibles N+1

**Estado:** verificado por inspección; no se ejecutó benchmark.

- Inventarios, movimientos, exhibiciones, depositantes, veteranos y varias relaciones usan `findAll()` y construyen listas completas; por ejemplo [`InventarioService.java:69`](backend/src/main/java/com/proveedores/service/InventarioService.java#L69) y [`ExhibicionService.java:87`](backend/src/main/java/com/proveedores/service/ExhibicionService.java#L87).
- El mapeo de exhibiciones consulta relaciones por cada registro; los DTO ricos de objetos también pueden disparar múltiples consultas con `open-in-view=false`.
- Exportaciones PDF cargan resultados y generan `byte[]` en memoria sin límite global.
- **Impacto:** latencia creciente, presión de heap y timeouts con datos reales.

### PERF-002 — Búsquedas textuales no aprovechan índices B-tree

**Estado:** inferido por consultas y migraciones.

- Varias búsquedas usan `lower`, `unaccent` y patrones `%texto%`. Los índices B-tree simples no aceleran ese patrón.
- La búsqueda normalizada de depositantes incluye un fallback sobre `findAll()` y filtrado Java.
- No se encontraron índices trigram/GIN para los campos de búsqueda libre.
- **Impacto:** scans completos a medida que crezcan objetos, personas y depositantes.

### API-001 — Contrato de paginación inestable

**Estado:** verificado en ejecución.

- Backend registró el warning oficial de Spring por serializar `PageImpl` directamente; la estructura JSON no está garantizada entre versiones.
- **Impacto:** una actualización puede romper consumidores aunque compile.

### SEC-002 — No se observa validación explícita de audiencia JWT

**Estado:** inferido.

- La configuración valida issuer/firma mediante Resource Server, pero no se encontró un validador de `aud` para `museo-backend`.
- Si otro cliente del mismo realm puede obtener un token con roles realm, podría aceptarse en la API.
- **Impacto:** confusión de audiencia. Requiere confirmar configuración/mappers de producción de Keycloak antes de declararlo explotable.

### LOG-001 — `X-Request-Id` cliente se incorpora al MDC sin límites visibles

**Estado:** inferido.

- El filtro acepta el identificador enviado por el cliente y lo refleja en cabecera/MDC sin una normalización estricta observada.
- **Impacto:** valores enormes o caracteres de control pueden degradar/ensuciar trazas. Los mensajes sí se sanitizan parcialmente, pero el valor MDC no usa esa sustitución.

### FILE-003 — Retención y limpieza de binarios no definidas

**Estado:** verificado.

- La baja lógica de fotos conserva original y versión pública. No se encontró job de purga, reconciliación DB/filesystem ni reporte de huérfanos.
- **Impacto:** crecimiento indefinido del volumen y dificultad para cumplir una política de retención.

### DOC-001 — Documentación arquitectónica desactualizada

**Estado:** verificado.

- `frontend/ARCHITECTURE.md` describe Vite, React Router y variables `VITE_*`; la implementación real es Next.js App Router con `NEXT_PUBLIC_*`.
- La arquitectura backend aún describe un conjunto antiguo de migraciones, mientras existen V1–V28.
- README menciona parametrización de puerto Keycloak, pero Compose publica `8081` de forma fija.
- **Impacto:** errores de despliegue y onboarding; falsa confianza en procedimientos obsoletos.

## 6. Hallazgos de prioridad baja

### BUILD-001 — Versiones no totalmente reproducibles

- `package.json` usa `latest` para varias dependencias; el lockfile fija la instalación actual, pero regenerarlo puede cambiar drásticamente el grafo.
- Imágenes como `postgres:16`, `keycloak:25.0`, Node/Maven/Temurin no están fijadas por digest o parche.

### OPS-002 — Servicios internos publicados en todas las interfaces

- PostgreSQL se publica en `0.0.0.0:5432`; Keycloak y backend también publican directamente puertos ([`docker-compose.yml:9`](docker-compose.yml#L9), [`docker-compose.yml:32`](docker-compose.yml#L32), [`docker-compose.yml:65`](docker-compose.yml#L65)).
- En producción, PostgreSQL no debería publicarse al host/Internet; backend y Keycloak deberían quedar detrás de un ingress/TLS según la topología elegida.

### API-002 — Nombre original en `Content-Disposition`

- Algunos downloads construyen la cabecera usando el nombre proporcionado originalmente. El nombre físico es seguro/aleatorio, pero conviene usar el builder RFC correspondiente para impedir cabeceras inválidas con nombres atípicos.

## 7. Seguridad

### Controles correctos comprobados

- API stateless con JWT; CSRF desactivado es coherente mientras la credencial siga en `Authorization` y no en cookie automática ([`SecurityConfig.java:41`](backend/src/main/java/com/proveedores/security/SecurityConfig.java#L41)).
- CORS usa orígenes explícitos, métodos/cabeceras limitados y `allowCredentials=false` ([`SecurityConfig.java:99`](backend/src/main/java/com/proveedores/security/SecurityConfig.java#L99)). El perfil prod exige declarar orígenes.
- Un request anónimo a objetos devolvió `401`.
- Un `VIEWER` que llamó directamente `POST /api/categorias` recibió `403`: ocultar controles en frontend no es la única defensa.
- `OPERATOR` recibió `403` en endpoint administrativo; `ADMIN` recibió `200`.
- Los endpoints de hijos de objeto consultan por `id` y `objetoId` en fotos/recibos, reduciendo IDOR entre padres.
- Los errores 400/401/403/404/409/500 tienen formato controlado. El handler genérico devuelve “Error interno del servidor” y no expone SQL ni stack trace ([`GlobalExceptionHandler.java:182`](backend/src/main/java/com/proveedores/exception/GlobalExceptionHandler.java#L182)); `server.error` también deshabilita detalles ([`application.yml:57`](backend/src/main/resources/application.yml#L57)).
- Swagger está deshabilitado en `application-prod.yml` ([`application-prod.yml:34`](backend/src/main/resources/application-prod.yml#L34)).
- El frontend usa Authorization Bearer, flujo estándar con PKCE S256 y no persiste el token en localStorage.

### Riesgos de seguridad

- **Crítico:** bypass de la política de originales (SEC-001).
- **Crítico si se despliega Compose:** usuarios/passwords y secreto de desarrollo versionados; Keycloak dev sin persistencia (IAM-001). Los valores no se reproducen aquí.
- **Alto:** dependencias web vulnerables (DEP-001).
- **Alto:** rol `MUSEOLOGO` incoherente/no aprovisionable (IAM-002).
- **Medio:** audiencia JWT no validada explícitamente (SEC-002).
- **Medio:** validación débil en familias de archivos distintas de fotos (FILE-001).
- **Medio:** request ID confiado al cliente sin límites claros (LOG-001).

### Control de acceso por objeto (IDOR/BOLA)

No se encontró un concepto de propietario/tenant por objeto: todo usuario con rol lector tiene, por diseño, alcance global sobre el catálogo permitido. Dentro de ese modelo, fotos y recibos validan la pertenencia del hijo al objeto, por lo que cambiar sólo el ID del hijo no permite cruzar de padre. Sí existe un fallo de autorización de **variante** de recurso: el endpoint común decide servir el original según un conjunto de roles más amplio que el endpoint explícito; éste es el SEC-001 verificado.

No se pudo validar un modelo de acceso por colección, sede o custodio porque no existe una política de ese tipo en el código. Si el negocio lo requiere, el sistema carece hoy de ese control.

## 8. Matriz de permisos

La matriz solicitada revela una discrepancia estructural: `MUSEOLOGO` no existe en el realm importado ni en el frontend. El rol real con escritura es `OPERATOR`.

| Operación principal | ADMIN | MUSEOLOGO | VIEWER | Frontend vs backend / evidencia |
|---|---:|---:|---:|---|
| Autenticarse | Sí | No aprovisionable por la app | Sí | Frontend y realm sólo tipan ADMIN/OPERATOR/VIEWER. |
| Leer catálogo general de objetos | Sí | **No** | Sí | Backend general excluye MUSEOLOGO; frontend no lo reconoce. |
| Ver campos privados de objeto | Sí | Helper: sí; endpoint general: no | No | Configuración incoherente para MUSEOLOGO. |
| Listar/descargar foto pública | Sí | Sí sólo en matcher específico | Sí | Coincide para endpoint fotográfico. |
| Ver foto marcada `PRIVADO` | Sí | Sí | No (404) | Política del servicio. |
| Descargar original por endpoint `/original` | Sí | No | No | Regla HTTP correcta. |
| Descargar original por endpoint común | Sí | **Sí según servicio** | No | **Fallo:** OPERATOR también lo consigue; MUSEOLOGO lo conseguiría si existiera. |
| Crear/editar objetos | Sí | No | No | El rol real autorizado es OPERATOR. VIEWER directo fue probado: 403. |
| Baja/restauración de objetos | Sí | No | No | OPERATOR tiene mutación general; acciones admin específicas pueden restringir más. |
| Leer depositantes/ubicaciones | Sí | No | No | Backend restringe a ADMIN/OPERATOR; frontend los presenta como operativos. |
| Administrar ubicaciones | Sí | No | No | Backend exige ADMIN. |
| Leer categorías/tablas auxiliares | Sí | No | Backend: sí | Frontend oculta esas pantallas a VIEWER: frontend más restrictivo que backend. |
| Administrar categorías y catálogos | Sí | No | No | Métodos/controladores exigen ADMIN aunque el matcher general admita OPERATOR. |
| Crear/editar exhibiciones, inventario, colecciones | Sí | No | No | El rol real de escritura es OPERATOR. |
| Eliminar colección | Sí | No | No | Regla específica ADMIN. |
| Ver movimientos/recibos/pendientes | Sí | No | No | ADMIN/OPERATOR solamente. |
| Administrar usuarios/roles | Sí | No | No | `/api/admin/**` ADMIN. |

**Conclusión de permisos:** el backend prevalece y sí bloquea mutaciones de VIEWER. La nomenclatura y comportamiento `MUSEOLOGO` deben definirse antes de producción; actualmente no puede sustituirse silenciosamente por `OPERATOR`, porque sus permisos observados no son equivalentes.

## 9. Base de datos

### Estado del esquema

- PostgreSQL real: imagen `postgres:16`.
- Flyway contiene **28 migraciones** y una base vacía se construyó correctamente hasta V28 mediante `FlywayPostgresIntegrationTest`.
- Hibernate opera con `ddl-auto=validate`, evitando modificaciones implícitas del esquema.
- Se observaron PK/FK, `NOT NULL`, índices y restricciones de unicidad en las migraciones. Las relaciones principales tienen integridad referencial.
- La numeración de inventario usa incremento atómico PostgreSQL, reduciendo duplicados concurrentes.

### Riesgos

- DB-001: unicidad global incompatible con borrado lógico/reactivación en varias relaciones.
- EXH-001: falta una garantía de base para solapamientos de exhibición.
- INV-001: la API permite reescribir el objeto de un inventario y el historial de movimientos.
- No se encontraron campos `@Version`; actualizaciones concurrentes pueden sobrescribirse con estrategia “última escritura gana”.
- Los índices existentes no cubren eficientemente búsquedas con `%texto%`, `lower/unaccent`.

### Migraciones

- **Verificado:** orden V1–V28, aplicación desde cero correcta, sin checksum roto en la ejecución de integración.
- Existen migraciones que corrigen/añaden índices parciales (por ejemplo colecciones, relaciones activas y embargos), pero no todas las restricciones históricas globales fueron reemplazadas; de allí DB-001.
- No se modificó ninguna migración histórica.
- **No verificable:** migración sobre un dump real de producción con su volumen, cardinalidad y calidad de datos, porque no se proporcionó una copia anonimizada ni un entorno descartable de producción.

## 10. Backend

### Tecnologías reales

- Java configurado: **17**, no Java 21.
- Spring Boot **3.3.5**, Maven, Spring Data JPA, Spring Security Resource Server, Bean Validation, Flyway, PostgreSQL, Testcontainers **1.21.3**, springdoc **2.6.0**, iText **7.2.5**, TwelveMonkeys **3.15.2**.

### Evaluación

- La estructura controller/service/repository/DTO/mapper/entity es clara y la mayoría de mutaciones está transaccionada.
- `open-in-view=false` es correcto para detectar accesos lazy fuera de servicio, aunque exige cuidar mappers y consultas.
- El manejo de `Optional` y not-found es generalmente explícito.
- `GlobalExceptionHandler` cubre validación, conflictos, autenticación/autorización y fallback 500 sin filtrar detalles internos.
- Se encontraron transacciones compuestas correctas, por ejemplo inventario + movimiento, y rollback/limpieza robusta en el flujo principal de fotos.

### Problemas relevantes

- Suite de integración rota (TEST-001).
- Reglas duplicadas entre servicios agregados y endpoints directos permiten inconsistencias (EXH-002, INV-001).
- Auditoría parcial (AUD-001).
- Listados masivos/N+1 (PERF-001).
- Respuesta `PageImpl` inestable (API-001).
- Restauraciones y borrados no se comportan uniformemente entre entidades.
- La build empaquetada pasa sólo al omitir tests; la build de calidad completa no pasa.

## 11. Frontend

### Tecnologías reales

- Next.js **16.2.5**, React, TypeScript, App Router y Node 22 Alpine en Docker.
- No es una aplicación Vite/React Router pese a lo indicado por documentación antigua.

### Verificaciones

- `npm run lint`: correcto.
- `npx tsc --noEmit`: correcto.
- `npm run build`: correcto; generación de producción completada.
- No existe script/framework de tests frontend en `package.json`; no hubo pruebas unitarias, de componentes ni E2E para ejecutar.

### Permitir edición

**Verificado por inspección, no por E2E:** el estado inicia en `false`; sólo ADMIN/OPERATOR pueden habilitarlo; al desactivarlo en una ruta que requiere edición se ejecuta `router.replace("/")`; el contenido protegido se oculta durante la redirección ([`editing-mode-provider.tsx:30`](frontend/src/lib/editing-mode/editing-mode-provider.tsx#L30), [`editing-mode-provider.tsx:46`](frontend/src/lib/editing-mode/editing-mode-provider.tsx#L46)). Un refresh reinicia el modo en falso y provoca la misma redirección. Volver con Atrás a una ruta protegida vuelve a encontrarlo deshabilitado.

Además, el cliente API bloquea globalmente `POST/PUT/PATCH/DELETE` cuando el modo está apagado ([`client.ts:13`](frontend/src/lib/api/client.ts#L13)). Esto es una barrera de UX, no de seguridad; la autorización real del backend fue comprobada aparte.

Riesgos:

- No hay tests automatizados para rutas, formularios, hidratación, navegación, desactivación durante edición o pérdida de estado.
- Algunas pantallas con acciones inline no necesitan ruta de edición; dependen del `canEdit` del componente y del bloqueo global del cliente. La defensa backend sigue siendo necesaria.
- Dependencias runtime vulnerables (DEP-001).
- `NEXT_PUBLIC_API_BASE_URL` cae a localhost si no se define ([`client.ts:11`](frontend/src/lib/api/client.ts#L11)); el pipeline de producción debe exigir valores explícitos.

## 12. Fotografías y archivos

### Flujo de fotografías

- **Verificado por código/tests:** se conserva un original y se genera una versión pública limitada a 1600 px con marca de agua `logo-header.png`; se almacenan rutas separadas.
- La imagen se decodifica realmente, se compara el formato detectado con el MIME declarado, se limita el tamaño y el total de píxeles. Los nombres físicos son UUID.
- `ObjectFileStorageService` normaliza rutas, verifica que permanezcan bajo el directorio raíz y usa escritura temporal + movimiento atómico. No se observó path traversal en ese servicio.
- El volumen `backend_storage:/app/storage` persiste originales, públicos y recibos firmados en el Compose local.
- No hay mapeo estático de `/app/storage` en frontend/Nginx; el acceso pasa por API.

### Privacidad y permisos

- VIEWER no puede listar/descargar una foto marcada privada; la prueba funcional obtuvo `404`.
- **Fallo crítico:** ADMIN/OPERATOR/MUSEOLOGO son tratados como aptos para original en el endpoint común (SEC-001).
- La ruta explícita de original sí exige ADMIN, pero no corrige la ruta alternativa.

### Ciclo de vida y fallos

- La baja lógica conserva binarios; no hay purga/reconciliación.
- En fotos, la limpieza en rollback es mejor que en recibos y multimedia de veteranos.
- Si falla la marca de agua, la transacción principal intenta revertir metadatos/archivos creados; no se ejecutó inyección controlada de falla en el stack activo.
- La regeneración masiva captura errores individualmente, pero sólo cuenta errores; no deja un detalle persistente de cada archivo fallido.
- No se verificó carrusel/pantalla completa mediante navegador E2E. El backend sí diferencia visibilidad, pero entrega el original indebidamente a roles internos.

## 13. Docker e infraestructura

### Verificado en el stack local

| Servicio | Estado observado | Healthcheck | Puerto host |
|---|---|---|---|
| PostgreSQL | Up, healthy | `pg_isready` | `0.0.0.0:5432` |
| Keycloak | Up | Ninguno | `0.0.0.0:8081` |
| Backend | Up, healthy | readiness HTTP | `0.0.0.0:8080` |
| Frontend | Up | Ninguno | `0.0.0.0:3000` |

- Health, liveness y readiness del backend respondieron `200`.
- El discovery OIDC respondió `200`; frontend respondió redirección `307` esperable hacia el flujo de aplicación.
- El backend tiene root filesystem read-only y `/tmp` en tmpfs. El entrypoint arranca como root para ajustar permisos del volumen y luego ejecuta Java con usuario no root.
- Frontend corre con usuario `nextjs`; Keycloak con UID 1000. PostgreSQL conserva el usuario por defecto de la imagen.

### Riesgos

- Compose es explícitamente local según documentación; no hay manifiesto de producción listo.
- Keycloak dev, sin persistencia/healthcheck; no hay restart policies, TLS, reverse proxy, límites de recursos ni secrets manager.
- PostgreSQL no necesita publicar el puerto al host en una topología estándar.
- Los tags no están fijados por digest y varios son sólo major/minor.
- `docker compose down` normal conserva `postgres_data` y `backend_storage`; recrear Keycloak pierde estado no importado. `down -v` elimina DB y archivos.
- **No se ejecutó `down/up`** porque el stack contenía datos locales existentes y la auditoría prohibía alterarlos. La persistencia indicada se infiere de mounts/volúmenes inspeccionados.

## 14. Configuración PROD

Aspectos correctos:

- `application-prod.yml` exige datasource, issuer/JWK, CORS y secreto administrativo desde variables; no tiene defaults sensibles.
- Swagger/API docs están deshabilitados.
- SQL formatting está desactivado y logging en INFO.
- Errores Spring no incluyen mensaje, bindings, excepción ni stack trace.

Bloqueantes/riesgos:

- Compose no activa el perfil prod.
- No existe validación de arranque documentada que rechace localhost o credenciales de ejemplo fuera de prod.
- No existe una definición de infraestructura productiva para Keycloak con PostgreSQL, TLS y hostname estricto.
- El frontend usa defaults localhost si faltan variables en build; al ser `NEXT_PUBLIC_*`, quedan incrustadas en el bundle.
- No se verificó un arranque completo con secretos/URLs reales de producción; no estaban disponibles y no deben inventarse.

## 15. Tests

| Prueba / comando | Resultado | Observaciones |
|---|---|---|
| `mvn test` | **FALLÓ** | 248 ejecutados; 209 exitosos; 4 fallidos; 35 errores; 0 ignorados. |
| Tests de integración | **FALLÓ** | 80 ejecutados; 42 exitosos; 3 fallidos; 35 errores. Muchas fixtures ya no cumplen validación de ficha completa. |
| Tests controller/seguridad | Parcial | 67 ejecutados; 66 exitosos; 1 fallo. Las suites explícitas `*SecurityTest` suman 39 y pasaron. |
| Tests unitarios de servicio/tiempo | Correcto | 101 ejecutados, 101 exitosos. |
| `FlywayPostgresIntegrationTest` | Correcto | Aplicó V1–V28 desde cero sobre PostgreSQL Testcontainers. |
| `mvn -DskipTests package` | Correcto | JAR Spring Boot generado. No es criterio suficiente de release porque omite la suite roja. |
| `npm run lint` | Correcto | Sin errores reportados. |
| `npx tsc --noEmit` | Correcto | Sin errores de tipos. |
| `npm run build` | Correcto | Next.js 16.2.5 generó la build de producción. |
| Tests frontend | **No disponibles** | No existe script/framework de tests. |
| `npm audit --json` | **FALLÓ por vulnerabilidades** | 24 totales: 1 crítica, 17 altas, 3 moderadas, 3 bajas; incluye dev. |
| `npm audit --omit=dev --json` | **FALLÓ por vulnerabilidades** | 5 runtime: 1 crítica, 3 altas, 1 moderada. |
| OWASP Dependency-Check Maven | No concluyente | Falló al actualizar NVD por falta de API key/datos; no se obtuvo inventario CVE fiable del backend. |
| Prueba API VIEWER → POST | Correcto | Respondió 403. |
| Prueba API anónima → GET | Correcto | Respondió 401. |
| Prueba OPERATOR → admin | Correcto | Respondió 403; ADMIN respondió 200. |
| Prueba privacidad foto VIEWER | Correcto | Foto privada respondió 404. |
| Prueba original vía endpoint común | **FALLÓ seguridad** | OPERATOR obtuvo los mismos bytes del original que ADMIN. |
| Health/readiness/liveness | Correcto local | Respondieron 200; no cubren Keycloak ni capacidad de escritura del storage. |

### Detalle de suites no exitosas

- `AdminObjetoMuseoControllerTest`: 1 fallo de 4.
- `ColeccionObjetoServiceIntegrationTest`: 8 errores de 10.
- `ExhibicionObjetoServiceIntegrationTest`: 2 errores de 2.
- `ExhibicionServiceIntegrationTest`: 3 errores de 3.
- `InventarioServiceIntegrationTest`: 1 error de 1.
- `ObjetoMuseoServiceIntegrationTest`: 21 errores y 3 fallos de 30.

### Gaps de cobertura relevantes

- No hay test que garantice que el endpoint común nunca entrega originales a no-ADMIN.
- No hay prueba concurrente de doble reserva de exhibición ni de actualizaciones perdidas.
- No hay cobertura suficiente de baja + reactivación bajo todas las restricciones únicas.
- No hay pruebas E2E/frontend de login/logout, modo edición, navegación, refresh, back, formularios o roles.
- No hay pruebas sistemáticas de MIME falso/corrupción para todas las familias de archivos, límites multipart, disco lleno y rollback DB/filesystem.
- No hay restauración real desde backup ni ensayo de desastre.
- No hay prueba de volumen/carga o queries N+1 con cardinalidad representativa.

## 16. Rendimiento

No se ejecutó benchmark porque no existe herramienta/carga preparada y no debía alterarse el stack. Los riesgos concretos son:

- Listados completos con `findAll()` en endpoints de dominio.
- Potenciales N+1 al mapear objetos/exhibiciones y sus relaciones.
- Exportes PDF en memoria con cardinalidad no acotada.
- Búsquedas `%texto%` y normalización en Java sin índices adecuados.
- Imágenes originales potencialmente grandes servidas desde el proceso backend; no hay CDN/object storage ni streaming/rate limiting documentado.
- Regeneración de fotos públicas en una sola operación síncrona que recorre todo el conjunto.

Antes de producción se deben medir las consultas críticas con datos comparables al volumen esperado y fijar límites/paginación.

## 17. Backups y recuperación

### Estado actual

- PostgreSQL y archivos están en volúmenes Docker separados.
- No se halló un procedimiento ejecutable de backup, restore o reconciliación.
- Keycloak no persiste su base en el Compose actual.
- No hay evidencia de restore probado.

### Escenarios de fallo

| Falla | Comportamiento/riesgo actual |
|---|---|
| PostgreSQL caído | Readiness backend falla; no hay restart policy ni runbook. |
| Keycloak caído | Backend puede seguir “ready”, pero login/admin fallan; Docker no lo marca unhealthy. |
| Backend caído | Frontend no tiene healthcheck ni fallback operativo; no hay restart policy. |
| Reinicio Docker | DB/fotos sobreviven si se conservan volúmenes; identidad Keycloak puede perder cambios. |
| Disco lleno | Escrituras fallan; no hay alerta de capacidad/readiness de storage. Algunos flujos pueden dejar archivo/DB desalineados. |
| Falla al guardar archivo | Fotos principales limpian mejor; recibos/multimedia pueden dejar huérfanos. |
| Falla al generar marca de agua | La carga de foto debe revertir, pero no fue probada por inyección en entorno integral. |
| Pérdida de volumen | Sin backup/restauración probados, pérdida potencialmente definitiva. |

Un backup no probado no se considera recuperabilidad. Éste es un bloqueante de salida.

## 18. Dependencias

### Frontend

- Audit completo: 24 vulnerabilidades (1 crítica/17 altas/3 moderadas/3 bajas).
- Runtime: 5 (1 crítica/3 altas/1 moderada).
- Next.js directo está afectado en la versión instalada; transitorias afectadas: `sharp`, `postcss`, `nanoid`, `baseline-browser-mapping`.
- El lockfile existe, pero `latest` en manifiesto aumenta el riesgo al regenerarlo.

### Backend

- La compilación/resolución Maven fue correcta.
- La auditoría CVE no pudo concluir por falta de dataset NVD/API key. No debe interpretarse como “sin vulnerabilidades”.
- Spring Boot 3.3.5 y el resto de versiones deben contrastarse en CI contra una fuente de vulnerabilidades habilitada antes de liberar.

## 19. Documentación

Documentación encontrada: README, `AGENTS.md`, arquitectura backend/frontend, deployment y checklist.

Fortalezas:

- `DEPLOYMENT.md` diferencia explícitamente el Compose local de producción.
- El checklist menciona perfil prod, secretos, TLS, backups, restore y healthchecks.

Faltantes/desactualizaciones:

- Arquitectura frontend describe un stack anterior.
- Arquitectura backend no refleja 28 migraciones ni todas las áreas actuales.
- No hay runbook operativo real, topología productiva, rotación de secretos, backup/restore, rollback de versión, respuesta a incidentes, monitoreo/alertas ni capacidad.
- No hay especificación única de roles; documentación/contexto y código difieren entre MUSEOLOGO y OPERATOR.
- No hay matriz oficial endpoint/rol ni política formal de originales/privados.
- No hay evidencia de que el checklist de deployment se haya ejecutado.

## 20. Deuda técnica relevante

- Reglas de negocio duplicadas entre servicios agregados y CRUD directos, con resultados distintos.
- Modelo de auditoría centrado sólo en objeto, insuficiente para operaciones administrativas y archivos.
- Falta de estrategia uniforme para soft delete/reactivación/unicidad.
- Contratos de listas/paginación heterogéneos y uso directo de `PageImpl`.
- Ausencia de tests frontend/E2E y fixtures backend mantenibles.
- Acoplamiento de binarios a filesystem local sin reconciliación/observabilidad.
- Documentación y roles divergentes del sistema real.

No se incluyen observaciones meramente estilísticas porque no cambian el riesgo productivo.

## 21. Checklist preproducción

- [x] Build backend sin tests genera JAR.
- [ ] Suite backend completa en verde.
- [x] Lint frontend.
- [x] Type checking frontend.
- [x] Build frontend de producción.
- [ ] Tests unitarios/componentes/E2E frontend.
- [x] Migraciones V1–V28 aplican desde cero.
- ⚠️ Migración validada con datos productivos anonimizados.
- [ ] Originales accesibles exclusivamente por la política definida.
- [x] VIEWER bloqueado para mutaciones directas verificadas.
- [ ] Rol MUSEOLOGO definido e implementado de extremo a extremo.
- [ ] Matriz oficial frontend/backend sin inconsistencias.
- [ ] Dependencias runtime sin vulnerabilidades críticas/altas aceptadas.
- ⚠️ Auditoría CVE backend concluida con feed disponible.
- [x] Errores API no exponen stack/SQL al cliente.
- [x] CORS explícito y CSRF coherente con Bearer stateless.
- [ ] Audiencia JWT validada/documentada.
- [x] Protección básica contra traversal en almacenamiento principal.
- [ ] Validación real uniforme de todos los tipos de archivo.
- [ ] Atomicidad/reconciliación DB + filesystem.
- [ ] Reglas concurrentes de exhibición garantizadas por DB/bloqueo.
- [ ] Soft delete + UNIQUE resuelto en todas las entidades.
- [ ] Auditoría de acciones sensibles completa.
- [ ] Compose/manifiesto productivo con perfil prod.
- [ ] Keycloak productivo, persistente, saludable y sin credenciales conocidas.
- [ ] PostgreSQL no publicado innecesariamente.
- [ ] TLS/reverse proxy/secret manager configurados.
- [ ] Healthchecks para frontend y Keycloak.
- [ ] Restart policies y límites de recursos.
- [x] Persistencia declarada para PostgreSQL y `/app/storage` en Compose local.
- [ ] Backup automático de DB, archivos e identidad.
- [ ] Restauración integral ensayada y documentada.
- ⚠️ Pruebas de volumen/rendimiento con datos representativos.
- [ ] Monitoreo, alertas de disco y runbook de incidentes.
- [ ] Documentación actualizada y aprobada.

## 22. Plan de correcciones recomendado

### P0 — Bloquea producción

| ID | Severidad | Componente / archivos | Descripción y evidencia | Impacto / reproducción | Recomendación | Dificultad |
|---|---|---|---|---|---|---|
| SEC-001 | Crítica | `FotoObjetoMuseoService.java`, `SecurityConfig.java` | Endpoint común sirve original a todo rol que ve privados; bytes idénticos verificados con OPERATOR. | Login OPERATOR → GET foto común → comparar con original ADMIN. Exposición de original. | Separar autorización “ver privado” de “descargar original”; test de seguridad negativo por cada rol y variante. | Baja-Media |
| TEST-001 | Crítica | Tests de objetos/inventario/colecciones/exhibiciones | 39/248 no exitosos; fixtures no alcanzan reglas bajo prueba. | Ejecutar `mvn test`. Sin gate fiable. | Corregir fixtures/expectativas sin relajar reglas; CI debe bloquear si falla cualquier test. | Media |
| DEP-001 | Crítica | `frontend/package.json`, lockfile | Audit runtime: 1 crítica, 3 altas, 1 moderada. | `npm audit --omit=dev`. Riesgo web conocido. | Evaluar advisories, actualizar a versiones corregidas y repetir build/E2E/audit. | Media |
| IAM-001 | Crítica | `docker-compose.yml`, realm Keycloak | `start-dev`, HTTP, defaults, secreto/cuentas versionadas, identidad no persistente. | Recrear contenedor puede reimportar estado conocido. | Crear despliegue prod separado: Keycloak `start`, DB persistente, TLS, secrets manager, realm sin usuarios/secretos de ejemplo, healthcheck. Rotar todo valor que se haya reutilizado. | Alta |
| OPS-001 | Crítica | Operación/infraestructura | Sin backup/restore implementados ni probados. | Pérdida de volumen/disco no recuperable. | Automatizar backups coordinados DB+archivos+Keycloak; cifrado, retención, RPO/RTO; ensayo documentado de restore. | Alta |

### P1 — Resolver antes de producción

| ID | Severidad | Componente / archivos | Descripción y evidencia | Impacto / escenario | Recomendación | Dificultad |
|---|---|---|---|---|---|---|
| IAM-002 | Alta | Realm, `KeycloakAdminService`, frontend session/permissions | MUSEOLOGO no aprovisionable y sólo aparece en reglas aisladas. | Token manual MUSEOLOGO: fotos sí, API general no. | Definir rol contractual; implementarlo en realm, admin, converter, backend, frontend y tests, o eliminarlo formalmente del modelo. | Media |
| EXH-001 | Alta | Servicios/repositorios/migración de exhibiciones | Check-then-insert sin lock/constraint. | Dos requests simultáneos reservan mismo objeto. | Serializar por objeto, usar lock apropiado o garantía DB; test concurrente. | Alta |
| EXH-002 | Alta | `ExhibicionObjetoService` | Endpoint directo omite solapamientos si exhibición planificada. | Crear relación por API en planes superpuestos. | Centralizar asociación en una sola regla de dominio o impedir CRUD directo inseguro. | Media |
| EXH-003 | Alta | `ExhibicionService` | Finalización/cancelación confirma devolución automáticamente. | Finalizar con objeto no devuelto produce “verificada”. | Exigir devolución/verificación explícita o registrar claramente liberación no verificada; conservar usuario y fecha. | Media |
| DB-001 | Alta | V1/V12 y servicios de relaciones/catálogos | Filas soft-deleted siguen bloqueando UNIQUE. | Quitar y volver a asociar categoría/exhibición/relación. | Política uniforme: reactivar fila o migrar a índices únicos parciales; nunca editar migraciones aplicadas, crear una nueva. | Media-Alta |
| INV-001 | Alta | `InventarioService`, `MovimientoInventarioService` | Inventario puede cambiar de objeto; movimientos históricos editables/borrables. | PUT con otro objeto/usuario/fecha reescribe historia. | Inmutabilidad del objeto y movimientos append-only; correcciones como eventos compensatorios auditados. | Media |
| AUD-001 | Alta | Servicios de dominio/auditoría | Falta auditoría de usuarios, archivos, inventario, depositantes, restauraciones y catálogos. | Operación sensible no reconstruible. | Catálogo formal de eventos; actor, timestamp, entidad, before/after, request ID; impedir mutación del log. | Alta |
| FILE-001 | Alta | Servicios de recibos/multimedia/storage | MIME declarado, archivos huérfanos y operaciones parciales. | Subir contenido falso o provocar rollback DB. | Detección por contenido, nombres/cabeceras seguras, cleanup afterCompletion y reconciliador. | Media-Alta |
| FILE-002 | Alta | `application*.yml`, exception handler | Límite multipart no alineado con 5/10 MB propios. | Foto de 2 MB puede rechazarse antes del servicio. | Configurar límites por perfil y mapear exceso a 413/400 estable; tests. | Baja |
| INFRA-001 | Alta | Compose/Actuator | Sin health de Keycloak/frontend, restart ni dependencia saludable. | Auth caído mientras backend figura ready. | Healthchecks, restart policy/orquestador, readiness de dependencias críticas y storage. | Media |
| SUPPLY-001 | Alta | CI Maven | CVE backend no evaluadas. | Dependency-Check sin NVD no produce resultado. | Configurar feed/API key/cache en CI o scanner equivalente con política de severidad. | Baja-Media |

### P2 — Recomendable antes o inmediatamente después del primer release controlado

| ID | Severidad | Componente | Descripción | Recomendación | Dificultad |
|---|---|---|---|---|---|
| PERF-001 | Media | API/JPA/PDF | Listas completas, N+1 y buffers grandes. | Paginar, projections/entity graphs, límites de export y medir SQL. | Media-Alta |
| PERF-002 | Media | PostgreSQL/búsqueda | `%texto%`/unaccent sin índice útil; filtro Java. | Índices funcionales/trigram verificados con `EXPLAIN ANALYZE`. | Media |
| API-001 | Media | API paginada | `PageImpl` con contrato inestable. | DTO explícito de página y contract tests. | Baja |
| SEC-002 | Media | JWT | Audiencia no validada explícitamente. | Validar `aud`/authorized party según diseño y probar tokens de clientes distintos. | Baja |
| LOG-001 | Media | Logging | Request ID cliente sin normalización/límite. | Aceptar sólo patrón/tamaño seguro o regenerar; propagar ID en body si se documenta. | Baja |
| FILE-003 | Media | Storage | Sin política de purga/reconciliación. | Job seguro, dry-run, métricas y retención aprobada. | Media |
| DOC-001 | Media | Documentación | Arquitectura y variables desactualizadas. | Regenerar docs desde implementación y revisar en CI/release. | Baja-Media |

### P3 — Mejora futura

| ID | Severidad | Componente | Descripción | Recomendación | Dificultad |
|---|---|---|---|---|---|
| BUILD-001 | Baja | Supply chain | Rangos `latest` y tags sin digest. | Versiones explícitas y actualizaciones automatizadas controladas. | Baja |
| OPS-002 | Baja/Media | Red | Puertos internos publicados a todas las interfaces. | Red interna y exposición sólo mediante ingress/firewall. | Baja |
| API-002 | Baja | Downloads | Nombre original en `Content-Disposition`. | Builder RFC 5987/6266 y sanitización de display name. | Baja |

## 23. Veredicto final

### ¿Pondrías este sistema en producción en su estado actual?

**NO.**

La exposición comprobada de originales, la suite backend con 39 resultados no exitosos, una vulnerabilidad crítica de runtime en frontend, el Keycloak de desarrollo/no persistente y la ausencia de restore probado constituyen bloqueantes independientes. La publicación sólo debería reconsiderarse cuando los P0 estén resueltos, los P1 de integridad/identidad hayan sido cerrados o aceptados formalmente con controles compensatorios, todos los tests estén verdes y se repita esta auditoría sobre la configuración productiva real.

---

### Limitaciones de esta auditoría

- No se ejecutó `docker compose down/up` ni `down -v` para no alterar datos existentes.
- No se ejecutaron pruebas destructivas de disco lleno, corrupción, caída de red o pérdida de volúmenes.
- No se contó con secretos, URLs, certificados ni manifiestos reales de producción.
- No se contó con dump anonimizado de tamaño productivo ni navegador E2E configurado.
- La auditoría CVE backend quedó inconclusa por indisponibilidad del feed NVD/API key.
- No se realizaron cambios correctivos, actualizaciones ni refactors, conforme al alcance solicitado.
