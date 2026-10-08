# Fase 5 — Remediación de auditoría final

Fecha de validación: 2026-10-07/08  
Rama de trabajo: `preprod/fases-1-2-integridad`  
Línea base: HEAD `ddd53ba`; cambios de Fases 4 y 5 aún pendientes de commit.

## 1. Resultado

Se corrigió el hallazgo bloqueante de integridad de exhibiciones y la carrera residual entre asociaciones y cambios de estado. También se implementaron las remediaciones de código para auditoría operativa, limpieza segura del seed histórico, mínimo privilegio PostgreSQL, actualización de Keycloak, endurecimiento del proxy, aislamiento de borradores y reducción del artefacto frontend.

La validación local quedó verde: 325 tests backend, lint/tipos/build frontend, stack productivo de seis servicios, Flyway V30, roles DB no privilegiados, aprovisionamiento de realm, backup y restore real.

Esto no constituye todavía autorización de go-live. Permanecen condiciones externas: escaneo Maven con una `NVD_API_KEY` válida, escaneo/SBOM de imágenes, copia off-host real, TLS/DNS institucional, alertas programadas y commit/tag inmutable seguido de auditoría independiente.

## 2. Integridad de exhibiciones

### EXH-FINAL-001

- La baja lógica bloquea la exhibición y sus objetos antes de decidir.
- Una exhibición no puede eliminarse si conserva reservas o devoluciones pendientes.
- Una relación pendiente sigue bloqueando disponibilidad aunque el padre histórico figure eliminado; sólo una devolución efectivamente verificada libera el objeto.
- La regresión `exhibicionFinalizadaNoPuedeDarseDeBajaNiLiberarObjetoConDevolucionPendiente` comprueba el estado final en PostgreSQL.
- `devolucionVerificadaPermiteBajaYReservaPosterior` comprueba el camino válido posterior a la verificación física.

### EXH-FINAL-002

- Se agregó `PESSIMISTIC_WRITE` para la exhibición padre.
- Asociación, sincronización, finalización, cancelación, inicio automático y operaciones de devolución toman/revalidan el padre dentro de la transacción.
- El orden de locks es padre y luego IDs de objetos ordenados.
- Se agregaron regresiones concurrentes reales para asociación contra cancelación, asociación contra finalización y edición contra finalización, además de la carrera entre dos reservas.

## 3. Base y datos demo

- V29 elimina el grafo demo de V2 sólo si reconoce su huella completa e intacta y no existen referencias o extensiones posteriores.
- Si el seed fue modificado o usado como dato real, la migración lo preserva y exige revisión manual.
- Los catálogos estructurales se conservan.
- Los fixtures requeridos por integración viven exclusivamente en `backend/src/test/resources/db/testdata`.
- `FlywayV28UpgradeIntegrationTest` cubre tanto la limpieza del demo intacto como la preservación del seed utilizado.
- V30 generaliza `auditorias` para entidades internas y referencias externas de Keycloak sin romper el historial de objetos.

Validación sobre base vacía del stack productivo:

```text
flyway=30
demo_objects=0
```

## 4. Auditoría operativa

Se agregaron eventos para:

- administración de usuarios Keycloak, roles, habilitación y reset de contraseña, incluidos fallos sin secretos;
- creación, actualización, restauración y baja de depositantes;
- alta, cambio y baja de inventario;
- estados y devoluciones de exhibiciones;
- carga, reemplazo, visibilidad, descarga sensible y baja de fotos/recibos;
- imágenes y referencias de video de veteranos.

La auditoría externa de Keycloak usa transacciones independientes para conservar el resultado aunque la operación remota falle. No se registran contraseñas, tokens, secretos ni contenido binario.

## 5. Keycloak e identidad

- Servidor actualizado de 25.0.6 a 26.8.0 y Admin Client a 26.0.12.
- Se ensayó previamente la migración de una copia 25.0.6 y se documentó rollback por restauración completa, no downgrade de esquema.
- El provisionador idempotente creó el realm sin usuarios de aplicación ni credenciales demo.
- Estado verificado: roles exactos `ADMIN,MUSEOLOGO,VIEWER`, tres clientes esperados y discovery OIDC HTTP 200.
- El cambio de roles agrega y confirma los nuevos roles antes de retirar los anteriores, reduciendo el riesgo de dejar una cuenta sin acceso ante fallo parcial.

## 6. PostgreSQL con mínimo privilegio

- Museo separa bootstrap, migrador/owner y runtime.
- Keycloak separa bootstrap y owner runtime.
- El backend conecta con runtime y Flyway con migrador.
- Se incluyeron scripts explícitos para instalaciones nuevas y conversión de volúmenes anteriores.
- Los tres roles de aplicación verificados tienen `rolsuper=false`, `rolcreatedb=false`, `rolcreaterole=false` y `rolbypassrls=false`.
- El runtime Museo no tiene `CREATE` sobre el schema `public`.

## 7. Proxy y frontend

- Hosts desconocidos se rechazan en HTTP/HTTPS; HTTP válido redirige sólo los hosts configurados.
- `X-Forwarded-For` se sobrescribe con la IP observada en el borde.
- Se agregaron CSP, anti-framing, Permissions-Policy y ocultación de `X-Powered-By`.
- Nginx usa el resolver DNS de Docker para no conservar IPs obsoletas tras recrear frontend, backend o Keycloak. La regresión operativa recreó frontend sin reiniciar el proxy y HTTPS continuó respondiendo.
- Next.js usa salida standalone; la imagen runtime no contiene `shadcn` ni `@modelcontextprotocol`.
- Los borradores se segmentan por subject, expiran a los siete días y se eliminan al cerrar sesión.

## 8. Backup y restore

Backup validado:

```text
backup-20261008T023117Z
museo.dump: OK
keycloak.dump: OK
storage.tar.gz: OK
metadata.txt: OK
```

La restauración destructiva se ejecutó sólo contra el stack aislado. Resultado:

- ambas bases recreadas con sus owners no privilegiados;
- Flyway restaurado en V30;
- realm `museo` presente;
- storage restaurado y storage anterior preservado;
- seis servicios saludables;
- aplicación HTTPS respondió 307 al flujo de login y discovery OIDC respondió 200;
- tiempo total observado: **173 segundos**.

`replicate-backup.sh` soporta filesystem montado independiente o `rsync` por SSH con host key estricta. No se contó con NAS/host remoto institucional, por lo que la condición de copia off-host real permanece abierta.

## 9. Pruebas ejecutadas

| Prueba | Resultado |
|---|---|
| `mvn clean test` | 325 ejecutados, 0 fallos, 0 errores, 0 omitidos |
| Tests afectados dirigidos | 29/29 OK |
| `npm run lint` | OK |
| `npx tsc --noEmit` | OK |
| `npm run build` | OK, 39 páginas estáticas |
| `npm audit --omit=dev --json` | 0 vulnerabilidades runtime |
| `bash -n ops/*.sh ops/keycloak/*.sh ops/postgres/*.sh` | OK |
| `docker compose ... config --quiet` | OK |
| Build y arranque productivo | 6/6 servicios healthy |
| Nginx `-t` | OK |
| Flyway/seed | V30, cero objetos `MM-DEV-*` |
| Backup + SHA-256 | OK |
| Restore real aislado | OK, 173 s |
| `git diff --check` | OK |

El health-check detectó correctamente que el certificado autofirmado del laboratorio vence dentro del umbral de 30 días. Esa alerta es evidencia del control, no validación de TLS productivo.

## 10. Pendientes antes de go-live

1. Versionar todos los cambios, revisar el commit y crear un tag candidato inmutable.
2. Repetir auditoría independiente sobre ese commit/tag exacto.
3. Ejecutar Dependency-Check con `NVD_API_KEY` real y resolver hallazgos aplicables.
4. Generar SBOM y escanear las imágenes exactas del release; fijar digests aprobados.
5. Replicar un backup a medio físicamente independiente y restaurar desde esa copia.
6. Instalar y validar DNS/certificado real, renovación y cadena completa.
7. Programar health-check/backup/réplica con un canal efectivo de alertas.
8. Validar sizing y exportaciones con volumen patrimonial representativo.

## 11. Veredicto de esta fase

Las correcciones de software P0/P1 abordadas por Fase 5 están implementadas y pasan la validación local. El candidato continúa **NO APTO PARA GO-LIVE** hasta cerrar las condiciones externas y repetir la auditoría sobre un release versionado e inmutable.
