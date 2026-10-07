# Fase 1 — Correcciones Preproducción

Fecha de validación: 2026-10-06.

## 1. Resumen

Se resolvieron exclusivamente `SEC-001`, `TEST-001`, `DB-001` e `IAM-002`.

- El endpoint común de fotografías ahora entrega siempre la versión pública destinada a visualización. El original sólo se obtiene mediante el endpoint explícito protegido para `ADMIN`.
- El rol operativo se unificó de extremo a extremo como `MUSEOLOGO`; `OPERATOR` dejó de ser un rol funcional aceptado o aprovisionable.
- Los flujos afectados por borrado lógico y unicidad reactivan la fila histórica cuando representa la misma identidad o relación. Los duplicados activos producen conflicto HTTP 409.
- Los fixtures de integración de objetos fueron actualizados para representar una ficha mínima válida sin debilitar las pruebas negativas.
- La suite backend terminó con 262/262 pruebas exitosas. Lint, type checking y build de producción del frontend terminaron correctamente.
- Testcontainers construyó PostgreSQL desde cero y Flyway aplicó correctamente V1–V28.

## 2. SEC-001

### Causa

`FotoObjetoMuseoService.descargar()` usaba el mismo criterio para “puede ver contenido privado” y “puede obtener el archivo original”. Como `OPERATOR`/`MUSEOLOGO` estaban incluidos en el primer criterio, el endpoint común podía cargar `rutaOriginal` y devolver exactamente los bytes privados.

### Solución

- `descargar()` carga exclusivamente `rutaPublica`, genera esa versión si falta, informa `original=false` y aplica el cache configurado para la imagen pública.
- `descargarOriginal()` usa el helper independiente `puedeDescargarOriginal()`, que acepta únicamente `ROLE_ADMIN`.
- `puedeVerPrivados()` conserva a `ADMIN` y `MUSEOLOGO`, por lo que el museólogo sigue pudiendo visualizar una foto privada, pero recibe la versión pública con marca de agua.
- La regla de seguridad del endpoint `/api/objetos/{objetoId}/fotos/{fotoId}/original` permanece antes que el matcher general y exige `ADMIN`.
- La consulta de una foto valida simultáneamente `fotoId` y `objetoId`; conocer o cambiar solamente `imagenId` no permite cruzar de objeto.
- No existe `ResourceHandler`, ruta estática ni publicación Docker para `/app/storage`. El volumen sólo está montado en el backend.
- `FotoObjetoMuseoResponseDTO` no contiene `rutaOriginal` ni `rutaPublica`; el mapper continúa anulando el nombre interno almacenado.

### Archivos principales

- `backend/src/main/java/com/proveedores/service/FotoObjetoMuseoService.java`
- `backend/src/main/java/com/proveedores/security/SecurityConfig.java`
- `backend/src/test/java/com/proveedores/service/FotoObjetoMuseoServiceTest.java`
- `backend/src/test/java/com/proveedores/controller/ObjetoPendienteCompletarSecurityTest.java`

### Tests y evidencia

- `ADMIN` obtiene 200 y bytes originales en el endpoint explícito.
- `MUSEOLOGO`, `VIEWER` y el rol legado `OPERATOR` obtienen 403; anónimo obtiene 401.
- El endpoint común devuelve bytes públicos tanto para `ADMIN` como para `MUSEOLOGO`/`VIEWER`.
- La prueba de foto privada demuestra que `MUSEOLOGO` la visualiza, pero los bytes públicos `{4,5,6}` no coinciden con los originales `{1,2,3}` y nunca se invoca la carga de `rutaOriginal`.
- Una foto privada continúa oculta para `VIEWER`.

## 3. TEST-001

### Causa de los fallos

La ejecución inicial confirmó 250 pruebas, 6 fallos y 35 errores. Los 35 errores provenían de fixtures de integración creados antes de la regla de ficha completa: carecían de descripción técnica, materiales, dimensión, estado de conservación o categoría. Los seis fallos eran expectativas obsoletas o mocks desactualizados:

- `AdminObjetoMuseoControllerTest` simulaba el overload antiguo de `listarEliminados(Pageable)` mientras el controlador llama `listarEliminados(String, Pageable)`.
- Pruebas de duplicados esperaban `BusinessException`/400, aunque un duplicado activo es correctamente un conflicto 409.
- Pruebas de recepción pretendían alcanzar validaciones de préstamo/comodato usando antes una ficha incompleta.

No se relajó `validarFichaCompleta()` ni ninguna regla productiva.

### Fixtures modificados

Se creó `ObjetoMuseoTestFixture`, con:

- `valido(...)`: ficha mínima válida reutilizable;
- `completar(...)`: conserva los valores específicos del escenario y completa solamente los campos obligatorios ausentes.

Las pruebas negativas que verifican objetos deliberadamente incompletos siguen construyendo sus DTO inválidos directamente.

### Bugs reales encontrados

Los problemas productivos encontrados durante esta fase correspondían a `SEC-001`, `DB-001` e `IAM-002` y se corrigieron en sus secciones respectivas. No apareció otro bug productivo dentro de los 41 resultados inicialmente fallidos: `TEST-001` era deuda de fixtures y expectativas.

### Resultado final

`mvn test`: **262 ejecutados, 262 exitosos, 0 fallidos, 0 errores, 0 ignorados**.

## 4. DB-001

La estrategia aplicada distingue identidad/relación única de una ocurrencia histórica.

| Entidad/relación | Problema encontrado | Estrategia | Resultado |
|---|---|---|---|
| Categoría | `UNIQUE(nombre)` bloqueaba recreación tras baja | Reactivar por nombre | Conserva ID; activo duplicado → 409 |
| Ubicación | `UNIQUE(nombre)` bloqueaba recreación tras baja | Reactivar por nombre | Conserva ID; activo duplicado → 409 |
| Rango militar | `UNIQUE(fuerza,nombre)` global | Reactivar por fuerza + nombre | Conserva ID; activo duplicado → 409 |
| Unidad militar | `UNIQUE(fuerza,nombre)` global | Reactivar por fuerza + nombre | Conserva ID; activo duplicado → 409 |
| Detalle de conservación | `UNIQUE(codigo)` global | Reactivar por código normalizado | Conserva ID; activo duplicado → 409 |
| Colección | Ya existe índice único parcial de activas, pero crear generaba otra fila | Reactivar la primera coincidencia eliminada | Conserva ID e historial; activo duplicado → 409 |
| Objeto–categoría | `UNIQUE(objeto,categoria)` global | Reactivar la asociación eliminada | Conserva ID y actualiza observación |
| Objeto–depositante | `UNIQUE(objeto,depositante)` global | Reutilizar/reactivar al actualizar recepción | Conserva ID e historial |
| Objeto–persona/veterano | `UNIQUE(objeto,veterano,tipo)` global | Reactivación ya existente; se normalizó duplicado activo a 409 | Conserva ID |
| Objeto–exhibición | `UNIQUE(exhibicion,objeto)` global | Reactivar asociación y reinicializar sus datos | Conserva ID; activo duplicado → 409 |
| Inventario por objeto | `UNIQUE(objeto)` global | Reactivar inventario y registrar nuevo ingreso | Conserva ID; activo duplicado → 409 |
| Relaciones objeto–objeto | El negocio conserva ocurrencias históricas | Mantener fila histórica y crear una nueva activa | Usa el índice parcial existente de V12; IDs distintos |
| Depositante DNI/CUIT | Flujo especial existente | Sin cambio: conflicto identificable + restauración explícita | Tests existentes conservan ID y datos |
| Embargos | Índices parciales existentes | Sin cambios necesarios | No se amplió alcance |

Las carreras residuales que alcancen una restricción de PostgreSQL son traducidas por `GlobalExceptionHandler` a HTTP 409, no 500.

Cobertura focalizada DB-001: 72 pruebas de integración PostgreSQL, 0 fallos y 0 errores, incluyendo creación, baja, recreación/reactivación, conservación de ID, duplicado activo y ocurrencia histórica.

## 5. IAM-002

### Keycloak

- El realm versionado define únicamente `ADMIN`, `MUSEOLOGO` y `VIEWER`.
- El usuario operativo de desarrollo y su asignación usan `MUSEOLOGO`.
- No se agregó acceso funcional transitorio para `OPERATOR`.

Importar nuevamente el JSON no debe considerarse una migración fiable de un realm productivo existente. Debe ejecutarse el procedimiento soportado siguiente mediante Admin Console, Admin REST API o `kcadm.sh`:

1. Exportar/respaldar el realm y registrar todos los usuarios que actualmente tienen `OPERATOR`.
2. Crear el realm role `MUSEOLOGO` si aún no existe.
3. Antes de desplegar el backend nuevo, asignar `MUSEOLOGO` a cada usuario identificado, sin retirar todavía `OPERATOR`.
4. Verificar en un entorno previo que un token renovado contenga `MUSEOLOGO`, mantenga operaciones habituales y no permita endpoints `ADMIN` ni originales.
5. Desplegar coordinadamente realm/configuración, backend y frontend; forzar renovación de sesión o nuevo login.
6. Verificar que ningún usuario dependa exclusivamente de `OPERATOR`.
7. Retirar `OPERATOR` de los usuarios y finalmente eliminar el rol del realm.

No se escriben tablas internas de Keycloak ni se usa Flyway para esta migración.

### Backend

- `SecurityConfig`, políticas de visibilidad y tests usan `MUSEOLOGO` como rol operativo.
- `KeycloakAdminService` sólo acepta/crea `ADMIN`, `MUSEOLOGO` y `VIEWER`.
- Intentar crear o asignar `OPERATOR` es rechazado.
- `MUSEOLOGO` conserva permisos operativos, pero no permisos administrativos ni descarga de originales.

### Frontend

- Modelos, validación de sesión, guards, rutas, menús, permisos, etiquetas y administración de usuarios usan `MUSEOLOGO`.
- `Permitir edición` deriva de `canWrite`, ahora habilitado para `ADMIN` y `MUSEOLOGO`.
- La lógica existente de `EditingModeProvider` se preservó: al desactivar el modo en una ruta de edición, redirige inmediatamente a `/` y no renderiza la pantalla protegida.

### Tests

- Permisos operativos validados con `MUSEOLOGO`.
- Acciones administrativas siguen limitadas a `ADMIN`.
- Administración de usuarios acepta `MUSEOLOGO` y rechaza el rol legado.
- Una prueba negativa explícita conserva un token `OPERATOR` únicamente para demostrar que ya no obtiene el original.

## 6. Archivos modificados

### Backend productivo

- Seguridad/IAM: `SecurityConfig.java`, `ObjetoVisibilityPolicy.java`, `KeycloakAdminService.java`, `FotoObjetoMuseoService.java`, `docker/keycloak/museo-realm.json`.
- Repositorios DB-001: `CategoriaObjetoRepository.java`, `ColeccionObjetoRepository.java`, `ExhibicionObjetoRepository.java`, `ObjetoCategoriaRepository.java`, `ObjetoDepositanteRepository.java`, `RangoMilitarRepository.java`, `UbicacionRepository.java`, `UnidadMilitarRepository.java`.
- Servicios DB-001: `CategoriaObjetoService.java`, `ColeccionObjetoService.java`, `DetalleConservacionService.java`, `ExhibicionObjetoService.java`, `ExhibicionService.java`, `InventarioService.java`, `ObjetoMuseoService.java`, `ObjetoVeteranoService.java`, `RangoMilitarService.java`, `RelacionObjetoService.java`, `UbicacionService.java`, `UnidadMilitarService.java`.

### Backend tests

- Controladores/seguridad: `AdminObjetoMuseoControllerTest.java`, `AdminObjetoMuseoSecurityTest.java`, `ColeccionObjetoSecurityTest.java`, `ComodatoPrestamoAdminControllerSecurityTest.java`, `ObjetoPendienteCompletarSecurityTest.java`, `RelacionObjetoSecurityTest.java`.
- Integración: `ColeccionObjetoServiceIntegrationTest.java`, `ExhibicionObjetoServiceIntegrationTest.java`, `ExhibicionServiceIntegrationTest.java`, `InventarioServiceIntegrationTest.java`, `ObjetoMuseoServiceIntegrationTest.java`, `RelacionObjetoServiceIntegrationTest.java`.
- Servicios: `FotoObjetoMuseoServiceTest.java`, `KeycloakAdminServiceTest.java`, `ObjetoMuseoServiceTest.java`.
- Nuevos: `integration/SoftDeleteUniqueCatalogIntegrationTest.java`, `testfixture/ObjetoMuseoTestFixture.java`.

### Frontend

- `src/models/session.ts`
- `src/lib/auth/auth-provider.tsx`
- `src/lib/auth/permissions.ts`
- `src/lib/auth/role-labels.ts`
- `src/lib/routes/index.ts`
- `src/features/usuarios/schemas.ts`
- `src/features/usuarios/types.ts`
- `src/features/usuarios/utils.ts`
- `src/app/objetos/[id]/page.tsx`

### Documentación actualizada

- Backend: `AGENTS.md`, `API.md`, `DEPLOYMENT.md`, `PROMPTS.md`, `SECURITY.md`, `architecture.md`.
- Frontend: `ARCHITECTURE.md`, `PROMPTS.md`.
- Nuevo reporte: `PRE_PRODUCTION_FIX_PHASE_1.md`.

`PRE_PRODUCTION_AUDIT.md` no fue sobrescrito ni modificado.

## 7. Migraciones

No se creó una migración Flyway nueva y no se modificó ninguna migración histórica.

La inspección verificó que la única relación revisada que necesita múltiples ocurrencias históricas (`relaciones_objetos`) ya fue resuelta por V12 mediante un índice único parcial para filas activas. Los demás constraints globales representan identidades o relaciones únicas y se resolvieron reactivando la fila existente desde los servicios. Flyway V1–V28 se validó desde una base PostgreSQL vacía.

## 8. Tests ejecutados

| Comando | Resultado | Cantidad / observaciones |
|---|---|---|
| Tests focalizados de fotografías y seguridad | OK | 27/27; roles, endpoint explícito, endpoint común y bytes distintos |
| Tests focalizados IAM/seguridad | OK | 79/79 |
| `mvn -q -Dtest=SoftDeleteUniqueCatalogIntegrationTest,ColeccionObjetoServiceIntegrationTest,ExhibicionObjetoServiceIntegrationTest,InventarioServiceIntegrationTest,ObjetoMuseoServiceIntegrationTest,RelacionObjetoServiceIntegrationTest test` | OK | 72/72 con PostgreSQL Testcontainers |
| `mvn test` | OK | 262 ejecutados; 262 exitosos; 0 fallidos; 0 errores; 0 ignorados |
| Flyway desde cero | OK | PostgreSQL 16 Testcontainers; V1–V28 validadas y aplicadas |
| `npm run lint` | OK | ESLint sin errores ni warnings reportados |
| `npx tsc --noEmit` | OK | Sin errores de tipos |
| `npm run build` | OK | Next.js 16.2.5; compilación y TypeScript correctos; 39 páginas estáticas generadas |
| `git diff --check` | OK | Sin errores de whitespace |

El frontend no define actualmente un script de tests automatizados adicional en `package.json`.

## 9. Problemas descubiertos

No se corrigieron los siguientes puntos por estar fuera del alcance de Fase 1:

- **Media — API-001 ya auditado:** Spring vuelve a advertir que serializar `PageImpl` directamente no garantiza un contrato JSON estable.
- **Baja — dependencia de logging:** el arranque de tests advierte que `commons-logging.jar` está presente junto con `spring-jcl`; debe investigarse en una fase de dependencias, sin actualizar paquetes aquí.
- **Operativo — migración IAM:** los usuarios de un Keycloak productivo existente deben migrarse con el procedimiento anterior antes del despliegue; el realm JSON versionado sólo deja correcto el estado de instalaciones nuevas.

No se abordaron exhibiciones, inventario, auditoría, archivos generales, infraestructura, backups, rendimiento, API ni logging fuera de lo estrictamente necesario para los cuatro IDs de esta fase.

## 10. Estado de la fase

- [x] `ADMIN` descarga el original mediante el endpoint explícito.
- [x] `MUSEOLOGO` y `VIEWER` no descargan originales.
- [x] El endpoint común nunca entrega el original.
- [x] Las fotos privadas conservan su política de visibilidad.
- [x] `MUSEOLOGO` reemplaza funcionalmente a `OPERATOR` sin adquirir privilegios `ADMIN`.
- [x] No se crean usuarios nuevos `OPERATOR`.
- [x] Existe un procedimiento soportado para migrar usuarios existentes.
- [x] `Permitir edición` funciona para `ADMIN`/`MUSEOLOGO` y redirige a Inicio al desactivarse en una ruta de edición.
- [x] Soft delete + reactivación conserva ID donde corresponde; duplicados activos siguen bloqueados.
- [x] Flyway aplica desde cero.
- [x] Backend, lint, tipos y build frontend están verdes.

**FASE 1 COMPLETADA**
