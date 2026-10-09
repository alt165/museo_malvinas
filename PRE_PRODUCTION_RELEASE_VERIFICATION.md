# Verificación final de release preproducción

Fecha de ejecución: 2026-10-08 (America/Argentina/Salta)
Alcance: verificación de solo lectura del código del release; las únicas mutaciones funcionales se realizaron con datos sintéticos en stacks Docker aislados. No se corrigió código, no se modificaron migraciones o dependencias y no se hizo commit ni push.

Estados usados:

- **IMPLEMENTADO:** el control existe en el commit.
- **VERIFICADO:** además fue ejercitado o inspeccionado con evidencia en esta ejecución.
- **PENDIENTE OPERATIVO:** requiere infraestructura, credenciales o decisiones externas al repositorio.
- **NO VERIFICABLE:** no hubo evidencia o capacidad suficiente para afirmarlo.

## 1. Commit auditado

- Rama: `preprod/fases-1-2-integridad`.
- Commit exacto: `9bb3ea3e0fabb3f9c9bdbe3bccad765feff87caf`.
- Asunto: `fix: complete pre-production phases 4 and 5`.
- `git ls-remote origin refs/heads/preprod/fases-1-2-integridad` devolvió el mismo hash completo. **VERIFICADO** en el remoto.
- Los commits inmediatamente anteriores son `ddd53ba` (Fase 3) y `89b862b` (Fases 1 y 2). Las Fases 4 y 5 están versionadas en el commit auditado.
- No hay tag apuntando al commit. El identificador inmutable por hash existe, pero el tag de release continúa como tarea de gobierno del despliegue.

## 2. Estado Git

- `git status --porcelain=v1 --untracked-files=all` estaba vacío antes de iniciar. **VERIFICADO**.
- No había archivos productivos sin seguimiento. `.phase5-runtime/` está ignorado y contiene exclusivamente artefactos sintéticos preexistentes de laboratorio.
- La segunda comprobación, antes de crear este informe, volvió a estar limpia. Después de la auditoría, el único cambio intencional es este informe sin commit.
- El inventario de archivos sensibles sólo encontró las plantillas versionadas `.env.production.example` y `frontend/.env.example`.
- Una búsqueda heurística sobre archivos versionados no encontró claves privadas, patrones AWS ni tokens Slack. No hay `gitleaks`, `trivy`, `grype` o `syft` instalados, por lo que esto no equivale a un escaneo especializado de todo el historial.

Resultado: **VERIFICADO**, con la limitación de escaneo especializado indicada.

## 3. EXH-FINAL-001 — exhibición finalizada con devolución pendiente

Resultado: **VERIFICADO / CERRADO (P0)**.

Evidencia automatizada:

- `ExhibicionServiceIntegrationTest`: 8 tests, 0 fallos. Incluye el bloqueo de baja y nueva reserva mientras la devolución está pendiente, y el camino permitido después de verificarla.
- La suite usó PostgreSQL real mediante Testcontainers.

Evidencia HTTP y PostgreSQL sobre el stack productivo aislado:

1. Se creó una exhibición `FINALIZADA`, una relación `PENDIENTE_REVISION`, `devolucion_verificada=false`, y una segunda exhibición para intentar reservar el mismo objeto.
2. `DELETE /api/exhibiciones/900001` devolvió **HTTP 409** con el mensaje de asignación/devolución pendiente.
3. `POST /api/exhibiciones-objetos` para la nueva exhibición devolvió **HTTP 409**.
4. PostgreSQL conservó: padre `FINALIZADA`, `eliminado=false`; relación `PENDIENTE_REVISION`, `devolucion_verificada=false`, `eliminado=false`; cero reservas nuevas.
5. `POST /api/exhibiciones-objetos/900001/verificar-devolucion` devolvió **200**.
6. La baja posterior devolvió **204** y la nueva reserva devolvió **201**.
7. Estado final: exhibición original `eliminado=true`; relación original `DEVUELTO`, verificada; relación nueva `EN_EXHIBICION` y activa.

No quedan P0 abiertos.

## 4. EXH-FINAL-002 — concurrencia

Resultado: **VERIFICADO / CERRADO (P1 anterior)**.

- `ExhibicionRepository.lockByIdForUpdate` usa `PESSIMISTIC_WRITE`.
- Las operaciones de asociación, devolución, edición, cancelación, finalización, inicio automático y baja bloquean/revalidan primero el padre.
- Los objetos se deduplican y bloquean por ID ordenado (`distinct().sorted()`), reduciendo inversión de orden y deadlocks.
- El estado del padre se valida dentro de la transacción después de tomar el lock.
- `ExhibicionConcurrencyIntegrationTest`: 4 tests, 0 fallos, PostgreSQL real:
  - asociación contra cancelación;
  - asociación contra finalización;
  - edición contra finalización;
  - doble reserva simultánea, repetida cinco veces, con exactamente un éxito, un conflicto y una sola relación activa.

## 5. Migraciones V1–V30

Resultado: **VERIFICADO**.

- V1–V28 coinciden con la línea base `23a8115`; el diff Fase 4/5 sólo agrega V29 y V30.
- V29 comprueba la huella completa del grafo demo y la ausencia de referencias adicionales antes de borrar. Ante cualquier divergencia emite advertencia y preserva los datos.
- V29 conserva los catálogos estructurales.
- V30 permite auditorías con `entidad_id` o `referencia_externa`, agrega un `CHECK` y un índice parcial por referencia externa.
- `FlywayV28UpgradeIntegrationTest`: 2 tests, 0 fallos:
  - V28 prístina: elimina los seis `MM-DEV-*`, conserva al menos seis categorías y llega a V30;
  - V28 con objeto modificado y fotografía agregada: conserva los seis objetos y la fotografía.
- Una base nueva real terminó en Flyway `30`, con `dev_objects=0` y `categories=6`.
- El restore aislado terminó nuevamente en Flyway `30`.

## 6. Auditoría operacional

Resultado: **IMPLEMENTADO Y VERIFICADO**.

Se trazaron llamadas de auditoría y sus pruebas para:

- creación, actualización, habilitación y deshabilitación de usuarios Keycloak;
- cambios de roles y fallos de cambios de roles;
- reset de contraseña temporal;
- altas, restauraciones, modificaciones y bajas de depositantes;
- inventarios y movimientos;
- exhibiciones, asociaciones, devoluciones, cancelaciones, finalizaciones y bajas;
- fotografías, descarga de original, visibilidad y baja;
- recibos escaneados y recibos firmados;
- imágenes y videos de veteranos.

Prueba real adicional:

- Se creó un usuario sintético por la API administrativa, se cambió de `VIEWER` a `MUSEOLOGO` y se reseteó su contraseña temporal: respuestas `201`, `200` y `204` respectivamente.
- PostgreSQL registró `USUARIO_CREADO`, dos `ROLES_ACTUALIZADOS` y `PASSWORD_TEMPORAL_RESETEADA` mediante `referencia_externa`.
- El reset sólo almacenó `{"credencialTemporalConfigurada":true}`.
- Una búsqueda de las contraseñas sintéticas, secretos sintéticos, `Bearer`, `access_token` y `refresh_token` en la auditoría devolvió cero coincidencias.
- El flujo P0 generó eventos de devolución verificada, baja de exhibición e incorporación a la nueva exhibición.
- `KeycloakAdminServiceTest` (12), `AuditoriaObjetoServiceTest` (3) y las integraciones de inventario, archivos, exhibiciones y multimedia pasaron dentro de la suite completa.

## 7. Keycloak

Resultado: **VERIFICADO** en entorno aislado.

- Imagen y ejecución: Keycloak **26.8.0**, `start --optimized`.
- Persistencia: PostgreSQL 16.11 separado; el realm sobrevivió al backup/restore.
- El aprovisionamiento creó `ADMIN`, `MUSEOLOGO` y `VIEWER`, sin usuarios de aplicación demo. Luego se agregaron usuarios/clientes exclusivamente sintéticos para las pruebas.
- Cliente backend `bearerOnly=true`; frontend público con Authorization Code, PKCE S256 y direct grants deshabilitados; cliente administrativo confidencial con service account.
- Mapper `museo-backend-audience`: audience incluida en access token.
- Token válido observado con `aud=["museo-backend","account"]`; el mismo endpoint administrativo respondió **401** a un JWT sin esa audience.
- Administración con token `ADMIN`: listado **200**, creación de usuario, cambio de roles y reset exitosos.
- Cambio seguro de roles: agregar, confirmar, quitar y confirmar estado final; la auto-remoción de ADMIN exige confirmación explícita. Las 12 pruebas unitarias pasaron.
- OIDC discovery por HTTPS respondió **200**.
- No se realizó un login interactivo completo en navegador; la emisión de tokens, validación de JWT, API y configuración PKCE sí fueron verificadas.

## 8. PostgreSQL

Resultado: **VERIFICADO**.

- Usuario runtime Museo `museo_app`: `SUPERUSER=false`, `CREATEDB=false`, `CREATEROLE=false`, `BYPASSRLS=false`, `CREATE` sobre esquema `public=false`.
- Usuario migrador `museo_migrator`: los cuatro atributos privilegiados en `false`.
- Usuario runtime Keycloak `keycloak_app`: los cuatro atributos privilegiados en `false`.
- Los usuarios bootstrap de cada clúster sí son administrativos, pero no son las credenciales runtime de backend ni Keycloak.
- Backend arrancó y Flyway aplicó V1–V30 usando las credenciales separadas configuradas para runtime y migración.

## 9. Seguridad web

Resultado: **VERIFICADO** en laboratorio.

- Host HTTP desconocido: conexión cerrada por Nginx (`444`; curl observa respuesta vacía/`000`).
- Host válido por HTTP: redirección `301` a HTTPS.
- Nginx reemplaza `X-Forwarded-For` por `$remote_addr`; no concatena el valor aportado por Internet.
- HTTPS entregó HSTS, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `X-Frame-Options: DENY`, `Permissions-Policy` y CSP con `frame-ancestors 'none'`.
- VIEWER: lectura **200**, escritura **403**, originales fotográficos **403**, administración **403**.
- MUSEOLOGO: lectura **200**, una escritura llegó a validación de payload **400** (no fue bloqueada por rol), originales **403**, administración **403**.
- ADMIN: administración **200**; originales están permitidos por la configuración de seguridad (las pruebas del backend cubren el control).
- JWT sin audience `museo-backend`: **401**. `JwtTokenValidatorTest`: 5 tests, 0 fallos.
- El certificado usado fue autofirmado/sintético. DNS, cadena y certificado institucional siguen pendientes operativos.

## 10. Frontend

Resultado: **VERIFICADO**, con un hallazgo nuevo de dependencias de desarrollo.

- `next.config.ts` usa `output: "standalone"`.
- La imagen runtime contiene `server.js` y sólo 12 directorios de `node_modules`; `typescript` y `eslint` no están presentes.
- La clave de borrador incluye el ID de usuario: `museo:user-draft:<usuario>:objeto-alta-completa`.
- Logout elimina los borradores del usuario actual.
- Los borradores vencen a los 7 días y los inválidos/vencidos son eliminados.

## 11. Tests backend

Comando: `mvn clean test`.

- **325 tests ejecutados**.
- **0 fallos, 0 errores, 0 omitidos**.
- `BUILD SUCCESS`, 2 min 24 s.
- Se conserva exactamente la línea base indicada de 325.

## 12. Tests frontend

- `npm ci`: ejecutado desde cero con `docker build --no-cache --target deps`; 687 paquetes instalados, salida 0. Se usó una etapa efímera para no reemplazar `frontend/node_modules` durante una auditoría de solo lectura.
- `npm audit --omit=dev`: **0 vulnerabilidades**.
- `npm run lint`: salida 0.
- `npx tsc --noEmit`: salida 0.
- `npm run build`: salida 0; compilación, TypeScript y 39/39 páginas estáticas completadas.
- El build productivo Docker también finalizó correctamente.
- El audit completo, que incluye herramientas de desarrollo/build, reportó 22 vulnerabilidades: 2 low, 3 moderate, 16 high y 1 critical. Ver hallazgos nuevos.

## 13. Docker productivo

Resultado: **VERIFICADO**.

- `docker compose ... config --quiet`: salida 0.
- Build exitoso de Keycloak, backend y frontend.
- Se levantó un proyecto aislado `museo-release-verification-9bb3ea3`, con datos bajo `/tmp` y puertos loopback no productivos.
- Los seis servicios alcanzaron `healthy`: `postgres-museo`, `postgres-keycloak`, `keycloak`, `backend`, `frontend`, `reverse-proxy`.
- Backend y frontend son read-only; sólo storage y tmpfs previstos son escribibles.
- El proxy fue el único servicio publicado.
- Se validaron PostgreSQL, Keycloak, backend, frontend, proxy y storage/backup sin datos reales.
- Los contenedores y redes efímeros fueron retirados al finalizar; los datos sintéticos quedaron fuera del repositorio.

## 14. Backup / restore

Resultado: **VERIFICADO** en entorno aislado.

- `backup.sh` y `restore.sh` pasan validación sintáctica y se revisaron sus protecciones de destino y confirmación.
- Se validó un backup preexistente: `museo.dump`, `keycloak.dump`, `storage.tar.gz` y `metadata.txt`, todos con checksum correcto.
- `restore.sh --confirm-restore` recreó exclusivamente `museo-phase5-dbtest` bajo `/tmp`.
- Restore completado en **113 s** con los seis servicios `healthy`.
- Estado posterior: Museo DB presente, realm `museo=1`, Flyway `30`, storage restaurado (el archivo contenía sólo la entrada raíz vacía, coherente con cero archivos restaurados).
- Después se ejecutó `backup.sh` del commit auditado. Generó `backup-20261008T094848Z` con `application_version=9bb3ea3e0fabb3f9c9bdbe3bccad765feff87caf`, Flyway 30, PostgreSQL 16.11 y todos los checksums correctos.
- El propio script advierte que una copia local no satisface disaster recovery. La réplica off-host real sigue abierta.

## 15. Dependencias y supply chain

- `NVD_API_KEY`: **ausente**.
- Dependency-Check Maven real: **NO VERIFICABLE / SUPPLY-FINAL-001 pendiente externo**. No se inventa un resultado.
- Existe workflow semanal y manual que exige el secreto y ejecuta `ops/dependency-check.sh`, pero no hay reporte válido disponible en esta máquina.
- `npm audit --omit=dev`: **VERIFICADO, 0**.
- Audit completo frontend: **22** vulnerabilidades en el árbol dev/build, incluida una crítica transitiva en `proxy-addr` y 16 altas. Todas desaparecen con `--omit=dev`, y las herramientas dev no están en la imagen standalone, pero participan en el proceso de build.
- Escaneo de imágenes y SBOM: **NO VERIFICABLE**. No existen herramientas ni automatización `trivy`, `grype`, `syft` o CycloneDX en el entorno/repositorio.

## 16. Pendientes operativos

| Control | Estado | Evidencia / condición pendiente |
|---|---|---|
| Backup/restore local | **VERIFICADO** | Restore completo y backup nuevo con checksums. |
| Backup off-host real | **PENDIENTE OPERATIVO (P1, OPS-FINAL-001)** | El script existe, pero no hubo NAS/host independiente ni restore desde esa copia. |
| DNS/certificado institucional | **PENDIENTE OPERATIVO (P2, TLS-FINAL-001)** | Sólo se usó certificado autofirmado de laboratorio. |
| Renovación TLS | **PENDIENTE OPERATIVO** | El health-check detectó correctamente que el fixture vence dentro de 30 días; no se probó renovación real. |
| Monitoreo programado | **IMPLEMENTADO, PENDIENTE OPERATIVO** | `health-check.sh` controla seis servicios, disco, TLS y antigüedad de backup; falta instalar la programación institucional. |
| Alertas efectivas | **PENDIENTE OPERATIVO (P2, OBS-FINAL-002)** | No se verificó canal de alerta/on-call. |
| NVD API key y escaneo Maven | **PENDIENTE OPERATIVO (P1, SUPPLY-FINAL-001)** | Secreto ausente. |
| Escaneo de imágenes y SBOM | **PENDIENTE OPERATIVO (P2, SUPPLY-FINAL-002)** | Sin scanner, SBOM ni aprobación de digests. |
| Sizing con volumen representativo | **PENDIENTE OPERATIVO (P2, PERF-FINAL-001)** | No se ejecutó carga patrimonial representativa. |
| Tag inmutable de release | **PENDIENTE OPERATIVO** | Commit remoto exacto verificado; no hay tag apuntando a HEAD. |

## 17. Hallazgos nuevos y estado de hallazgos anteriores

### Nuevo: FRONTEND-VERIFY-001 — vulnerabilidades en dependencias dev/build

- **Severidad:** P1 de supply chain de build.
- `npm ci` limpio reportó 22 vulnerabilidades totales: 1 crítica, 16 altas, 3 moderadas y 2 bajas.
- `npm audit --omit=dev` reportó cero, y la imagen standalone no contiene TypeScript, ESLint ni el toolchain completo; no se demostró exposición runtime.
- La vulnerabilidad crítica corresponde a `proxy-addr` transitivo; entre las altas hay dependencias transitivas y directas del toolchain (`eslint-config-next`, `postcss`, `shadcn`).
- No se actualizó ninguna dependencia por la restricción de solo lectura. Debe revisarse el lockfile, el contexto real de explotación durante build y aplicarse una actualización controlada antes del release.

### Resumen de severidades

- P0 abiertos: **0**.
- P1 anteriores cerrados y verificados: **EXH-FINAL-002, AUD-FINAL-001, DB-FINAL-001, IAM-FINAL-001, IAM-FINAL-002, REL-FINAL-001 y DB-FINAL-002**.
- P1 abiertos: **3** — `SUPPLY-FINAL-001`, `OPS-FINAL-001`, `FRONTEND-VERIFY-001`.
- P2 de software de la auditoría anterior verificados/cerrados: Host, X-Forwarded-For, cabeceras web, imagen frontend runtime y separación de borradores.
- P2/operativos abiertos: TLS/DNS real, renovación, monitoreo/alertas instalados, escaneo/SBOM de imágenes y sizing representativo.

## 18. Veredicto

# NO APTO PARA DESPLIEGUE

Las correcciones funcionales P0/P1 de exhibiciones, concurrencia, auditoría, migraciones, Keycloak, PostgreSQL y seguridad web están presentes y funcionaron en el commit auditado. Sin embargo, no corresponde declarar APTO porque permanecen P1 sin resolver: no existe un Dependency-Check Maven válido sin `NVD_API_KEY`, no existe una copia off-host restaurada y el audit completo del toolchain frontend presenta una vulnerabilidad crítica y 16 altas aún no evaluadas/remediadas. También faltan condiciones operativas de DNS/TLS institucional, renovación, alertas, escaneo/SBOM de imágenes y sizing.

La siguiente revisión debe ejecutarse sobre el mismo hash o un nuevo release inmutable, después de cerrar los P1 y aportar evidencia de las condiciones operativas previas al despliegue.
