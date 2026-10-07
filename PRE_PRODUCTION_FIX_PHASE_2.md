# Fase 2 — Integridad de dominio

## 1. Resumen

Esta fase corrige exclusivamente `EXH-001`, `EXH-002`, `EXH-003` e `INV-001`, tomando como línea base las correcciones de Fase 1.

El resultado principal es:

- las reservas de objetos se serializan por objeto mediante locks de fila de PostgreSQL dentro de la misma transacción que valida y persiste;
- todas las vías de asociación reutilizan una única autoridad de dominio y el `PUT` genérico de la relación fue eliminado;
- finalizar una exhibición ya no inventa una devolución física ni una verificación humana;
- cancelar una exhibición futura libera sus reservas sin registrar devoluciones inexistentes;
- el objeto al que pertenece un inventario es inmutable;
- los movimientos de inventario son de sólo lectura por API y se conservan como historial append-only;
- la suite backend pasó de la línea base de 262 a 280 tests, todos exitosos.

No se modificaron migraciones históricas ni fue necesario crear una migración nueva.

## 2. Modelo final de exhibiciones

### Reserva y asociación

Una relación `ExhibicionObjeto` activa representa la reserva o afectación de un objeto. Su ciclo de vida ya no se acepta desde el cliente: el backend deriva la fecha de inclusión, el estado y todos los campos de devolución a partir de la exhibición y de operaciones explícitas.

La asociación valida en una única autoridad de dominio:

- existencia y vigencia lógica de exhibición y objeto;
- estado válido de la exhibición;
- relaciones activas o eliminadas que puedan reactivarse;
- períodos incompatibles;
- reservas planificadas, exhibiciones activas y devoluciones pendientes;
- fechas temporales y permanentes.

### Disponibilidad y solapamiento

Se conserva la semántica inclusiva existente de fechas: dos períodos que comparten un día se solapan. Una exhibición permanente se trata como un intervalo sin límite superior.

Bloquean una nueva asignación:

- una exhibición `PLANIFICADA` o `ACTIVA` con período incompatible;
- una exhibición `FINALIZADA` cuya relación continúa pendiente de devolución/verificación.

No bloquean:

- una reserva de una exhibición futura correctamente cancelada;
- una relación cuya devolución fue verificada explícitamente;
- una relación o exhibición dada de baja/cancelada conforme a las reglas del agregado.

### Finalización, cancelación, devolución y verificación

- **Finalizar** es un cambio administrativo de la exhibición. Las relaciones no devueltas pasan a `PENDIENTE_REVISION`; no se completan usuario, fecha, retiro ni verificación.
- **Cancelar** sólo se admite para una exhibición `PLANIFICADA` que todavía no comenzó. Sus reservas se dan de baja lógica y no se registra una devolución física falsa.
- **Devolver y verificar** es una operación explícita. Registra el usuario autenticado/local identificable, fecha y hora, fecha de retiro y observaciones, y deja la relación `DEVUELTO` con `devolucionVerificada=true`.
- **Revertir una verificación** es una operación explícita y auditada que elimina esa evidencia y vuelve a bloquear la disponibilidad cuando corresponde.

Por decisión de integridad física, finalizar no libera por sí solo el objeto: permanece no disponible hasta una verificación explícita. La cancelación previa al inicio sí libera la reserva porque el objeto nunca fue registrado como salido físicamente.

## 3. EXH-001

### Causa

La implementación anterior consultaba disponibilidad y luego insertaba la asociación. Dos transacciones podían observar simultáneamente el objeto como disponible y persistir reservas incompatibles.

### Solución y estrategia de concurrencia

`ObjetoMuseoRepository.lockIdsForUpdate(...)` ejecuta un `SELECT id ... FOR UPDATE`, ordenado por ID. `ExhibicionObjetoService` adquiere esos locks antes de comprobar conflictos y los mantiene dentro de la misma transacción que crea, reactiva o sincroniza las asociaciones.

Se bloquea la fila del objeto, no toda la tabla. Cuando intervienen varios objetos se ordenan los IDs para reducir riesgo de deadlock. La sección crítica sólo contiene consultas y persistencia local; no incluye archivos, Keycloak ni operaciones externas.

Después de obtener el lock se vuelve a consultar el estado de reservas. Por ello, la segunda transacción ve la asignación confirmada por la primera y recibe un conflicto de dominio (`409` en la API), en lugar de persistir una segunda reserva o producir un `500`.

### Prueba concurrente

`ExhibicionConcurrencyIntegrationTest` usa PostgreSQL real mediante Testcontainers, dos transacciones/hilos coordinados con barreras y cinco carreras independientes. En cada carrera:

- exactamente una reserva termina con éxito;
- exactamente una termina con `ConflictException`;
- la consulta final a PostgreSQL encuentra una sola relación activa para el objeto.

Resultado: prueba exitosa en la suite final.

## 4. EXH-002

### Caminos alternativos encontrados

Existían dos implementaciones distintas:

- `ExhibicionService` aplicaba reglas temporales al crear o editar el agregado;
- el CRUD directo de `ExhibicionObjetoService` comprobaba reglas parciales y exponía un `PUT` capaz de reescribir campos de ciclo de vida.

### Centralización

`ExhibicionObjetoService` quedó como autoridad de asignación. Expone internamente el bloqueo, la validación de conflictos y la sincronización de relaciones. `ExhibicionService` delega allí la creación, edición y repetición/sincronización de objetos.

El DTO directo de asociación acepta únicamente `exhibicionId` y `objetoMuseoId`. El backend deriva el resto. El frontend fue adaptado al mismo contrato y ya no solicita fecha o estado de ciclo de vida al operador.

### Endpoints afectados

- `POST /api/exhibiciones-objetos`: conserva la asociación directa necesaria, pero pasa por exactamente los mismos locks e invariantes.
- `PUT /api/exhibiciones-objetos/{id}`: eliminado; responde `405`.
- `DELETE /api/exhibiciones-objetos/{id}`: sólo puede retirar una reserva planificada o una relación ya devuelta. Rechaza relaciones activas/finalizadas con devolución pendiente.
- la edición genérica de exhibición ya no puede forzar cambios de estado; se deben usar las operaciones específicas de ciclo de vida.

Se probaron conflictos mediante el endpoint directo, el servicio principal, la edición y períodos permanentes. Ninguna ruta conservada evita la regla temporal.

## 5. EXH-003

La implementación distingue explícitamente:

`finalización ≠ devolución ≠ verificación`.

Al finalizar, una relación no verificada pasa a `PENDIENTE_REVISION` y conserva nulos `verificadoPor`, `fechaVerificacion` y `fechaRetiro`. Tampoco se fuerza `devolucionVerificada=true`. Esto se aplica igualmente a finalizaciones anticipadas.

La devolución verificada sólo se produce mediante `POST /api/exhibiciones-objetos/{id}/verificar-devolucion`. El usuario no puede enviar ni suplantar un `usuarioId`: el backend resuelve al verificador desde la identidad autenticada mediante `UsuarioMovimientoService`. Se persisten usuario, instante, observaciones y fecha de retiro, y se utiliza la infraestructura de auditoría existente.

Una exhibición futura planificada debe cancelarse, no finalizarse. La cancelación da de baja lógica las reservas sin crear evidencia de devolución. Una exhibición ya iniciada se finaliza y queda pendiente de verificación física.

Los tests cubren ausencia de verificación automática, campos no inventados, conservación de observaciones, identidad y fecha del verificador, indisponibilidad mientras existe devolución pendiente, liberación posterior a verificación, cancelación previa al inicio y finalización anticipada.

## 6. INV-001

### Inmutabilidad del inventario

`InventarioService.actualizar(...)` compara el objeto persistido con `objetoMuseoId` del DTO. Un intento de cambiar la identidad devuelve `ConflictException` y la API responde `409`; el valor no se ignora silenciosamente.

### Historial append-only

`MovimientoInventarioController` y `MovimientoInventarioService` quedaron exclusivamente de consulta (`GET` de listado y detalle). Se eliminaron las operaciones CRUD genéricas de creación, edición y baja. Los intentos autenticados de `POST`, `PUT` o `DELETE` reciben `405`, independientemente de que el rol sea `ADMIN`, `MUSEOLOGO` o `VIEWER`.

Los movimientos nuevos sólo son generados por operaciones de negocio del inventario. Conservan usuario y fecha obtenidos en backend. La consulta continúa ordenada por fecha descendente.

### Corrección de errores históricos

Se utiliza el mecanismo compensatorio que ya permite el dominio: si un movimiento A → B fue incorrecto, se registra una nueva transición B → A mediante la operación de negocio del inventario. El movimiento original no se edita ni elimina. No se agregó una referencia `movimientoCorregidoId`, porque el modelo actual puede expresar la reversión sin alterar el historial y agregarla no era necesario para cerrar el hallazgo.

Los tests verifican que el historial conserva el original, agrega el compensatorio, mantiene orden, usuario y fecha, y que los movimientos automáticos continúan generándose.

## 7. Base de datos

- **Migraciones nuevas:** ninguna.
- **Migraciones históricas modificadas:** ninguna.
- **Garantía concurrente:** locks de fila PostgreSQL sobre `objetos_museo`, adquiridos con `FOR UPDATE` dentro de la transacción de asociación.
- **Constraints nuevos:** ninguno; la exclusión temporal depende de estado y devolución física, por lo que se resolvió mediante serialización por objeto y validación central posterior al lock.
- **Compatibilidad de datos:** no se borraron ni reinterpretaron registros históricos. No se marcaron devoluciones antiguas ni se inventaron verificadores.
- **Base desde cero:** verificada con PostgreSQL 16/Testcontainers; Flyway validó y aplicó V1–V28 sobre esquema vacío.
- **Upgrade desde V28:** no aplica un cambio de esquema en esta fase; el binario actualizado opera sobre el esquema V28 existente.

## 8. Archivos modificados

La lista siguiente corresponde únicamente a Fase 2; el árbol ya contenía los cambios no confirmados de Fase 1, que se preservaron.

### Backend productivo

- `backend/src/main/java/com/proveedores/controller/ExhibicionObjetoController.java`: elimina actualización genérica y evita recibir identidad del verificador.
- `backend/src/main/java/com/proveedores/controller/MovimientoInventarioController.java`: API de historial sólo lectura.
- `backend/src/main/java/com/proveedores/dto/ExhibicionObjetoRequestDTO.java`: contrato reducido a los IDs de asociación.
- `backend/src/main/java/com/proveedores/mapper/ExhibicionObjetoMapper.java`: elimina construcción insegura desde campos de ciclo de vida enviados por cliente.
- `backend/src/main/java/com/proveedores/mapper/MovimientoInventarioMapper.java`: elimina el mapeo de escritura de movimientos históricos.
- `backend/src/main/java/com/proveedores/repository/ObjetoMuseoRepository.java`: lock de fila determinista.
- `backend/src/main/java/com/proveedores/service/ExhibicionObjetoService.java`: autoridad central de asignación, devolución y disponibilidad.
- `backend/src/main/java/com/proveedores/service/ExhibicionService.java`: delegación de invariantes y separación entre finalizar/cancelar/devolver.
- `backend/src/main/java/com/proveedores/service/InventarioService.java`: identidad de objeto inmutable.
- `backend/src/main/java/com/proveedores/service/MovimientoInventarioService.java`: servicio de historial sólo lectura.

### Tests backend

- `backend/src/test/java/com/proveedores/controller/ExhibicionObjetoControllerSecurityTest.java`
- `backend/src/test/java/com/proveedores/controller/InventarioControllerIntegrityTest.java`
- `backend/src/test/java/com/proveedores/controller/MovimientoInventarioControllerSecurityTest.java`
- `backend/src/test/java/com/proveedores/integration/ExhibicionConcurrencyIntegrationTest.java`
- `backend/src/test/java/com/proveedores/integration/ExhibicionObjetoServiceIntegrationTest.java`
- `backend/src/test/java/com/proveedores/integration/ExhibicionServiceIntegrationTest.java`
- `backend/src/test/java/com/proveedores/integration/InventarioServiceIntegrationTest.java`
- `backend/src/test/java/com/proveedores/service/ExhibicionObjetoServiceTest.java`
- `backend/src/test/java/com/proveedores/service/ExhibicionServiceTest.java`
- `backend/src/test/java/com/proveedores/service/InventarioServiceTest.java`

### Frontend

- `frontend/src/features/exhibiciones/api/exhibiciones-api.ts`: elimina el `PUT` genérico.
- `frontend/src/features/exhibiciones/api/index.ts`: elimina su exportación.
- `frontend/src/features/exhibiciones/components/objetos-exhibicion-panel.tsx`: formulario alineado al contrato de asociación.
- `frontend/src/features/exhibiciones/schemas.ts`: elimina campos de ciclo de vida editables.
- `frontend/src/features/exhibiciones/types.ts`: actualiza el request a sólo IDs.

### Reporte

- `PRE_PRODUCTION_FIX_PHASE_2.md`: este documento.

## 9. Tests agregados

La suite aumentó de 262 a 280 tests. La cobertura nueva o ampliada incluye:

- carrera real de dos transacciones contra PostgreSQL, repetida cinco veces;
- solapamientos temporales y período permanente;
- conflicto por endpoint directo y por flujo principal;
- edición de fechas con objetos ya reservados;
- ausencia de `PUT` en asociaciones;
- derivación backend del estado de la relación;
- finalización normal y anticipada sin evidencia física falsa;
- devolución explícita con usuario, fecha y observaciones;
- indisponibilidad hasta verificar y liberación posterior;
- cancelación antes del inicio sin devolución falsa;
- intento de cambiar el objeto de un inventario;
- API de movimientos sólo lectura para `ADMIN`, `MUSEOLOGO` y `VIEWER`;
- conservación del movimiento original y creación compensatoria;
- generación automática, usuario, fecha y orden del historial.

## 10. Tests ejecutados

| Comando | Resultado | Tests/observaciones |
|---|---|---|
| `mvn clean test` (en `backend`) | OK | 280 ejecutados, 280 exitosos, 0 fallos, 0 errores, 0 omitidos. Incluye PostgreSQL/Testcontainers y la prueba concurrente. |
| `mvn test` final (en `backend`) | OK | 280 ejecutados, 280 exitosos, 0 fallos, 0 errores, 0 omitidos; ejecutado nuevamente después del último ajuste menor. |
| `npm run lint` (en `frontend`) | OK | Sin errores. |
| `npx tsc --noEmit` (en `frontend`) | OK | Sin errores de tipos. |
| `npm run build` (en `frontend`) | OK | Build de producción completada; 39 páginas estáticas generadas. |
| Flyway sobre PostgreSQL 16 vacío | OK | Testcontainers validó y aplicó las 28 migraciones V1–V28. |
| `git diff --check` | OK | Sin errores de whitespace. |

Los artefactos temporales generados por las verificaciones no se incorporaron al repositorio.

## 11. Regresiones

- `SEC-001`: los tests de fotografía y seguridad de Fase 1 permanecen verdes; no se modificó su política.
- Roles: `ADMIN`, `MUSEOLOGO` y `VIEWER` permanecen como matriz definitiva. La asociación y el inventario mantienen autorización backend; `MUSEOLOGO` no recibe privilegios exclusivos de administrador.
- Soft delete/reactivación: los tests de catálogos y relaciones de Fase 1 permanecen verdes.
- Backend completo: 280/280.
- Frontend: lint, TypeScript y build exitosos.
- Flyway: V1–V28 desde cero exitoso.

## 12. Problemas descubiertos

No se corrigieron los siguientes puntos porque no bloquean los cuatro hallazgos y pertenecen a fases posteriores:

| Severidad | Hallazgo | Alcance posterior |
|---|---|---|
| Media | La ejecución efectiva compila y prueba con Java 17, mientras el contexto operativo menciona Java 21. Debe alinearse y validarse el runtime de despliegue. | Dependencias/operación (`DEP-001`/`OPS-001`). |
| Baja | Spring advierte que serializar `PageImpl` directamente no garantiza un JSON estable. | Contrato/paginación (`API-001`/`PERF`). |
| Baja | Se observa advertencia de `commons-logging` junto a Spring JCL en el classpath de tests. | Higiene de dependencias (`DEP-001`). |
| Baja | El movimiento compensatorio preserva origen, destino, usuario y fecha, pero el esquema actual no enlaza formalmente el movimiento corregido ni posee un motivo estructurado específico. | Auditoría ampliada (`AUD-001`), si el negocio exige esa trazabilidad adicional. |

Ninguno de estos puntos habilita doble reserva, fabrica devoluciones ni permite reescribir movimientos por la API actual.

## 13. Decisiones de diseño

### Cuándo queda disponible un objeto

- Una reserva futura se libera al cancelar correctamente la exhibición antes de su inicio.
- Finalizar una exhibición iniciada no libera el objeto.
- El objeto queda disponible cuando la devolución se verifica explícitamente.
- Revertir la verificación vuelve a bloquearlo.

### Qué significa finalizar

Cerrar administrativamente la exhibición. Las piezas no verificadas quedan pendientes de revisión física.

### Qué significa devolver

Registrar mediante la operación específica que la pieza retornó. No se deduce del calendario ni del estado administrativo.

### Qué significa verificar

Una persona autenticada confirma la devolución. El sistema registra su identidad, el instante y las observaciones; esos datos no pueden ser enviados como identidad arbitraria por el cliente.

### Cómo se corrige un movimiento erróneo

Mediante un nuevo movimiento compensatorio creado por la operación de inventario. El registro original permanece inmutable y consultable. No existe `PUT` ni `DELETE` genérico de movimientos.

### Por qué se eligió lock pesimista por objeto

La incompatibilidad depende no sólo de rangos, sino también de estados administrativos y verificación física. Un exclusion constraint aislado no representa de forma simple toda esa semántica. El lock de la entidad estable `ObjetoMuseo`, seguido de una única validación central, proporciona exclusión efectiva entre todas las rutas sin infraestructura distribuida ni locks globales.

## 14. Estado

- [x] Dos requests concurrentes incompatibles no persisten dos reservas.
- [x] Existe prueba concurrente real con PostgreSQL/Testcontainers.
- [x] Todas las asociaciones conservadas pasan por las mismas invariantes.
- [x] El endpoint alternativo ya no evita las reglas temporales.
- [x] Finalizar no verifica ni inventa devolución.
- [x] Cancelar antes del inicio no crea evidencia física falsa.
- [x] La disponibilidad refleja la devolución verificada.
- [x] Un inventario no puede cambiar de objeto.
- [x] Los movimientos no pueden editarse ni eliminarse por CRUD.
- [x] Las correcciones preservan el movimiento original.
- [x] Los movimientos automáticos continúan funcionando.
- [x] Se mantienen las correcciones de Fase 1.
- [x] Backend: 280 tests, 0 fallos, 0 errores.
- [x] Frontend lint, TypeScript y build: OK.
- [x] Flyway V1–V28 desde cero: OK.

`FASE 2 COMPLETADA`
