# Auditoría Final Independiente Preproducción — Sistema Museo

Fecha de ejecución: 2026-10-07  
Rama inspeccionada: `preprod/fases-1-2-integridad`  
HEAD inspeccionado: `ddd53bae778a51c085b55c6af9d76770fe900c27`  
Alcance: código, tests, migraciones V1–V28, frontend, Keycloak, JWT, archivos, Docker productivo, proxy TLS, backup, restore, persistencia y resiliencia.

Las afirmaciones de los informes anteriores se usaron sólo como antecedentes. Los resultados de este documento provienen de inspección del estado actual y de pruebas ejecutadas durante esta auditoría. No se modificó código, configuración, tests, migraciones ni dependencias; sólo se creó este reporte y material temporal de prueba posteriormente eliminado.

## 1. Resumen ejecutivo

El sistema presenta avances verificables: backend 313/313 verde con Java 17; frontend compila; `npm audit --omit=dev` no informa vulnerabilidades runtime; JWT rechaza una audiencia distinta; la matriz ADMIN/MUSEOLOGO/VIEWER prevalece en backend; los originales fotográficos sólo se entregan a ADMIN; los uploads falsos, con MIME adulterado y sobredimensionados fueron rechazados; el locking concurrente cubierto por el test existente funciona; los movimientos de inventario no tienen endpoints de mutación; Flyway construye una base vacía hasta V28; el stack productivo aislado arrancó con seis servicios saludables; y backup/restore recuperó de manera real las dos bases y el storage.

Sin embargo, se reprodujo una violación bloqueante de integridad patrimonial: una exhibición finalizada con devolución aún pendiente puede darse de baja lógicamente; la búsqueda de conflictos ignora entonces esa exhibición y permite reservar el mismo objeto en otra exhibición. La secuencia real devolvió `200`, `204` y finalmente `201`, dejando al objeto administrativamente disponible sin verificación física.

También subsisten condiciones altas: una segunda carrera entre cambio de estado y asociación no revalida la exhibición después del lock; operaciones críticas de identidad y archivos carecen de auditoría; V2 inyecta datos de desarrollo en toda base productiva nueva; el aprovisionamiento de roles Keycloak es no atómico; Keycloak 25.0.6 está dos años detrás de la línea mantenida; el escaneo Maven sigue sin evidencia válida; las piezas completas de Fase 4 no están versionadas en HEAD; las aplicaciones usan usuarios PostgreSQL superusuario; y no existe evidencia de una copia de backup realmente fuera del servidor.

## 2. Veredicto

# 🔴 NO APTO PARA PRODUCCIÓN

Existe 1 hallazgo P0 comprobado que permite una doble afectación física/administrativa de patrimonio. Los 313 tests verdes no cubren esta composición de operaciones. Además quedan 9 hallazgos P1 que requieren resolución o evidencia operativa antes de un nuevo go-live review.

## 3. Hallazgos P0

### EXH-FINAL-001 — La baja de una exhibición finalizada elimina el bloqueo de una devolución pendiente

- **Severidad:** P0 — bloqueante.
- **Estado:** verificado por inspección y reproducido sobre PostgreSQL/stack productivo aislado.
- **Componente:** exhibiciones, disponibilidad e integridad física.
- **Archivos/líneas:** `backend/src/main/java/com/proveedores/service/ExhibicionService.java:289-300`; `backend/src/main/java/com/proveedores/service/ExhibicionObjetoService.java:234-255`.
- **Evidencia:** `bajaLogica()` sólo impide eliminar una exhibición `ACTIVA`; permite eliminar una `FINALIZADA` aunque sus relaciones sigan `PENDIENTE_REVISION`. `buscarConflicto()` descarta cualquier relación cuyo padre tenga `eliminado=true`, antes de evaluar que una devolución no verificada bloquea disponibilidad.
- **Reproducción real:** finalizar exhibición sembrada `1` → HTTP `200`; `DELETE /api/exhibiciones/1` con MUSEOLOGO → `204`; crear una nueva exhibición solapada con el objeto `2`, cuya devolución no fue verificada → `201`. La base terminó con la exhibición anterior eliminada y la nueva reserva activa.
- **Impacto:** el mismo objeto puede figurar reservado para otra exhibición aunque no exista evidencia de devolución física. Esto rompe la garantía central de Fase 2 y puede causar doble asignación, pérdida de trazabilidad o manipulación física indebida.
- **Recomendación:** impedir la baja del padre mientras exista cualquier relación no devuelta/verificada, o mantener dichas relaciones en la consulta de disponibilidad aun cuando el padre esté eliminado. Agregar test de integración para `finalizar → baja lógica → nueva reserva` y una constraint/invariante de dominio que no dependa de filtrar el padre.

## 4. Hallazgos P1

### EXH-FINAL-002 — Carrera entre asociación/edición y cambio de estado de exhibición

- **Severidad:** P1.
- **Estado:** confirmado por análisis de transacción; el interleaving exacto no está cubierto por el test concurrente existente.
- **Componente:** exhibiciones/concurrencia.
- **Archivos/líneas:** `ExhibicionObjetoService.java:59-73`; `ExhibicionService.java:198-227`.
- **Evidencia:** la asociación lee y valida el estado de la exhibición antes de bloquear el objeto. Si espera por el lock mientras otra transacción cancela/finaliza, luego continúa usando la entidad ya cargada sin refresh ni revalidación. La edición también lee estado/fechas antes del lock de objetos y no bloquea la fila de exhibición.
- **Impacto:** una asociación puede persistirse en una exhibición cancelada/finalizada, o una edición puede sobrescribir un cambio de estado concurrente.
- **Reproducción:** T1 carga exhibición PLANIFICADA; T2 bloquea objeto y cancela/commitea; T1 obtiene el lock y guarda usando estado obsoleto.
- **Recomendación:** bloquear también la exhibición con orden determinista, o refrescar y revalidar su estado dentro de la misma transacción después de adquirir los locks. Agregar una carrera real asociación-vs-cancelación y edición-vs-finalización.

### AUD-FINAL-001 — Trazabilidad incompleta de operaciones críticas

- **Severidad:** P1.
- **Estado:** verificado por búsqueda exhaustiva de llamadas.
- **Componente:** auditoría.
- **Archivos/líneas:** `AuditoriaObjetoService.java:23-74`; `KeycloakAdminUsuarioController.java:51-99`; `KeycloakAdminService.java:57-142`; servicios de fotos, recibos, depositantes, catálogos, veteranos e inventario.
- **Evidencia:** la infraestructura de auditoría sólo registra entidad `OBJETO_MUSEO`. No hay registro durable equivalente para crear/editar/habilitar usuarios, resetear contraseñas o cambiar roles; tampoco para cargas/bajas/visibilidad de fotos y recibos, catálogos, depositantes ni baja/reactivación de inventario.
- **Impacto:** acciones sensibles no pueden atribuirse ni reconstruirse de forma confiable ante incidente, disputa o error humano.
- **Reproducción:** ejecutar `PUT /api/admin/usuarios/{id}/roles` o `POST .../reset-password`; no se escribe una entrada en `auditorias`.
- **Recomendación:** ampliar el modelo de auditoría a identidad y recursos sensibles, con actor, fecha, acción, entidad, ID y cambios, evitando secretos.

### DB-FINAL-001 — Flyway V2 siembra datos de desarrollo en toda base nueva

- **Severidad:** P1.
- **Estado:** verificado en SQL y base productiva aislada.
- **Componente:** Flyway/datos productivos.
- **Archivo/líneas:** `backend/src/main/resources/db/migration/V2__seed_initial_data.sql:30-145`.
- **Evidencia:** V2 inserta veteranos, depositantes, seis objetos `MM-DEV-*`, inventarios/movimientos y una exhibición activa descritos explícitamente como “de prueba”. El arranque productivo desde vacío los incorporó.
- **Impacto:** contaminación del inventario patrimonial, numeración y reportes; posibilidad de confundir datos ficticios con bienes reales.
- **Reproducción:** iniciar PostgreSQL vacío con perfil prod y consultar `objetos_museo`/`exhibiciones` tras Flyway.
- **Recomendación:** definir un saneamiento forward-only conservador y un mecanismo separado de seed sólo para dev/test. No editar V2 histórica.

### IAM-FINAL-001 — Cambio de roles Keycloak no atómico

- **Severidad:** P1.
- **Estado:** verificado por inspección.
- **Componente:** administración de usuarios.
- **Archivo/líneas:** `backend/src/main/java/com/proveedores/service/KeycloakAdminService.java:121-142`.
- **Evidencia:** elimina primero todos los roles gestionados (`129-135`) y sólo después resuelve/agrega los nuevos (`137-139`). Un timeout o fallo entre ambas llamadas deja al usuario sin roles; puede afectar incluso a un administrador.
- **Impacto:** pérdida de acceso y posible bloqueo administrativo parcial.
- **Reproducción:** provocar fallo de red/Keycloak después de `realmRoles.remove()` y antes de `realmRoles.add()`.
- **Recomendación:** resolver previamente representaciones, aplicar diff seguro agregando antes de quitar cuando corresponda y compensar/registrar fallos parciales.

### IAM-FINAL-002 — Keycloak 25.0.6 fuera de la línea de seguridad actual

- **Severidad:** P1.
- **Estado:** verificado por imagen y releases oficiales; explotabilidad individual no determinada por scanner.
- **Componente:** IAM/supply chain.
- **Archivo/línea:** `ops/keycloak/Dockerfile` (base `quay.io/keycloak/keycloak:25.0.6`).
- **Evidencia:** 25.0.6 fue publicado en septiembre de 2024. A la fecha de auditoría, Keycloak publica 26.8.0 y las ramas 26.6/26.7 acumulan múltiples correcciones de seguridad, incluidas autenticación, autorización y OIDC. Referencias: [25.0.6](https://www.keycloak.org/2024/09/keycloak-2506-released), [26.7.5](https://www.keycloak.org/2026/09/keycloak-2675-released), [26.8.0](https://www.keycloak.org/2026/10/keycloak-2680-released).
- **Impacto:** el componente que controla toda identidad queda sobre una versión antigua sin evaluación de CVEs aplicables.
- **Recomendación:** analizar la ruta de upgrade soportada, migrar a una versión mantenida, ejecutar regresión de realm/tema/mapper y escaneo de imagen antes del go-live.

### SUPPLY-FINAL-001 — Auditoría de vulnerabilidades Maven no verificada

- **Severidad:** P1.
- **Estado:** no verificado; condición explícita pendiente.
- **Componente:** dependencias backend.
- **Archivo/línea:** `ops/dependency-check.sh:7`.
- **Evidencia:** `NVD_API_KEY` no estaba disponible; el script terminó antes del análisis con “NVD_API_KEY must be supplied...”. No existe reporte válido que permita afirmar ausencia de CRITICAL/HIGH.
- **Impacto:** riesgo desconocido en Spring, librerías transitivas y componentes backend.
- **Reproducción:** ejecutar `./ops/dependency-check.sh` sin el secret.
- **Recomendación:** ejecutar CI con feed/cache válido y NVD key secreta; bloquear por política CVSS y revisar falsos positivos de manera documentada.

### REL-FINAL-001 — La infraestructura productiva no forma parte del commit/branch auditado

- **Severidad:** P1.
- **Estado:** verificado con Git.
- **Componente:** release/reproducibilidad.
- **Archivos:** `docker-compose.prod.yml`, `ops/`, `.env.production.example`, `.github/`, `PRODUCTION_DEPLOYMENT.md`, `PRE_PRODUCTION_FIX_PHASE_4.md`.
- **Evidencia:** todos aparecen `??` en `git status`; HEAD y `origin/preprod/fases-1-2-integridad` siguen en el commit de Fase 3 `ddd53ba`. `.gitignore` y `README.md` también están modificados sin commit.
- **Impacto:** un checkout limpio del release no contiene la infraestructura, backups ni procedimientos que fueron validados. No es posible reproducir el despliegue desde Git.
- **Reproducción:** `git ls-files docker-compose.prod.yml ops` no devuelve entradas.
- **Recomendación:** versionar, revisar y etiquetar la infraestructura exacta; ejecutar la auditoría/CI sobre ese commit inmutable.

### DB-FINAL-002 — Credenciales runtime son superusuarios PostgreSQL

- **Severidad:** P1.
- **Estado:** verificado por configuración de imagen oficial.
- **Componente:** PostgreSQL/least privilege.
- **Archivo/líneas:** `docker-compose.prod.yml:8-15,30-37,113-115`.
- **Evidencia:** `POSTGRES_USER` crea el superusuario inicial y esas mismas credenciales se entregan al backend; el patrón se repite para Keycloak.
- **Impacto:** compromiso de backend/Keycloak o SQL injection tendría privilegios de superusuario de base, ampliando daño y opciones de ejecución/administración innecesarias.
- **Recomendación:** separar owner/migrator de usuarios runtime con permisos mínimos y rotación; conservar bases/usuarios separados como ya hace la topología.

### OPS-FINAL-001 — No hay evidencia de segunda copia off-host configurada

- **Severidad:** P1 — condición operativa.
- **Estado:** software preparado; operación real no configurada/verificable.
- **Componente:** disaster recovery.
- **Archivo/líneas:** `ops/backup.sh:17-28,141-142`; `.env.production.example`.
- **Evidencia:** el destino es configurable y el script advierte que una copia local no satisface DR, pero no replica a NAS/servidor/objeto remoto ni existe evidencia de job externo. La prueba se realizó localmente.
- **Impacto:** pérdida total del host/disco puede destruir datos y backups simultáneamente.
- **Recomendación:** configurar un destino físicamente independiente, cifrado y monitorizado; probar restauración desde esa copia, no desde el mismo host.

## 5. Hallazgos P2

### PROXY-FINAL-001 — Open redirect por Host no confiable

- **Severidad:** P2.
- **Archivo/líneas:** `ops/nginx/default.conf.template:6-18`.
- **Evidencia/reproducción:** `Host: attacker.example` sobre HTTP respondió `301 Location: https://attacker.example/probe` porque usa `$host` en el default server.
- **Impacto:** facilita phishing y redirecciones bajo la IP/dominio esperado.
- **Recomendación:** rechazar hosts desconocidos y redirigir exclusivamente a `${APP_HOST}`/`${AUTH_HOST}`.

### PROXY-FINAL-002 — Se conserva X-Forwarded-For aportado por Internet

- **Severidad:** P2.
- **Archivo/líneas:** `ops/nginx/default.conf.template:44-53,57-62,92-103`; `docker-compose.prod.yml:67`.
- **Evidencia:** `$proxy_add_x_forwarded_for` concatena el header del cliente; Keycloak confía `xforwarded`.
- **Impacto:** spoofing de IP en controles de brute force, eventos o futuras decisiones basadas en IP.
- **Recomendación:** en el proxy de borde sobrescribir XFF con `$remote_addr`, o configurar una cadena explícita de proxies confiables/real-ip.

### WEB-FINAL-001 — Headers de aislamiento web incompletos

- **Severidad:** P2.
- **Componente:** navegador/proxy.
- **Archivo/líneas:** `ops/nginx/default.conf.template:33-36,89-90`.
- **Evidencia:** HTTPS tiene HSTS/nosniff, pero no Content-Security-Policy ni `frame-ancestors`/X-Frame-Options para la app; la respuesta expone `X-Powered-By: Next.js`.
- **Impacto:** menor defensa ante XSS/clickjacking, especialmente relevante en SPA con token en memoria.
- **Recomendación:** definir CSP compatible, `frame-ancestors 'none'` o política institucional, Permissions-Policy y ocultar cabecera informativa.

### DEP-FINAL-001 — Toolchain vulnerable copiado dentro de la imagen frontend runtime

- **Severidad:** P2.
- **Componente:** frontend/supply chain.
- **Archivo/líneas:** `frontend/Dockerfile:1-4,21-32`.
- **Evidencia:** `npm audit --omit=dev` = 0, pero el audit completo informa 22 (1 critical, 16 high, 3 moderate, 2 low). El crítico `proxy-addr@2.0.7` llega por `shadcn → MCP SDK → express`. La imagen runner copia el `node_modules` completo instalado con devDependencies.
- **Impacto:** no se demostró ruta remota desde `next start`, pero aumenta innecesariamente superficie/tamaño y coloca paquetes vulnerables dentro del artefacto productivo.
- **Recomendación:** standalone output o prune/install production-only en runner; actualizar toolchain y volver a auditar.

### PRIV-FINAL-001 — Borrador con campos privados persiste entre usuarios del navegador

- **Severidad:** P2.
- **Componente:** frontend/privacidad.
- **Archivo/líneas:** `frontend/src/features/objetos/components/objeto-museo-form.tsx:145-180,329-377`; `frontend/src/lib/auth/auth-provider.tsx:90-98`.
- **Evidencia:** el borrador fijo `museo_alta_completa_objeto_borrador` guarda en `localStorage` descripción técnica, inscripciones, condición legal, conservación, visibilidades y depositante. Logout no lo elimina ni lo segmenta por usuario.
- **Impacto:** en un puesto compartido, el siguiente usuario del mismo navegador puede recuperar información privada de un borrador anterior.
- **Recomendación:** namespacing por subject, limpieza en logout/timeout y política explícita de expiración; evaluar no persistir campos privados.

### OBS-FINAL-001 — Fallos de dependencias provocan timeouts largos en el borde

- **Severidad:** P2.
- **Componente:** resiliencia/observabilidad.
- **Archivos/líneas:** `ops/nginx/default.conf.template:54,73,103`; configuración Hikari por defecto.
- **Evidencia:** con PostgreSQL Museo detenido, readiness a través del proxy no devolvió 503 en 5 s: agotó timeout (`000`); tras recuperar DB volvió a `200`. Con Keycloak detenido, discovery también agotó 5 s; readiness backend y API con token/JWK cacheados siguieron `200`.
- **Impacto:** respuestas colgadas y diagnóstico ambiguo durante incidentes.
- **Recomendación:** ajustar connect/read timeouts y pool timeout, definir respuestas 502/503 rápidas, y documentar degradación parcial.

### OBS-FINAL-002 — Monitoreo existe como script, no como servicio de alerta

- **Severidad:** P2 — condición operativa.
- **Componente:** operaciones.
- **Archivo/líneas:** `ops/health-check.sh:16-62`.
- **Evidencia:** el script comprobó seis servicios, disco, certificado y antigüedad del backup, pero compose no lo programa ni envía alertas. Readiness tampoco comprueba storage escribible/espacio disponible.
- **Impacto:** disco lleno, backup vencido o certificado por expirar pueden no ser detectados si el administrador no configura scheduler/alerta.
- **Recomendación:** instalar timer/cron y canal de alertas; agregar prueba de escritura/espacio de storage separada de liveness.

### PERF-FINAL-001 — Listados y exportaciones no acotados

- **Severidad:** P2.
- **Componente:** rendimiento/API.
- **Archivos/líneas representativas:** `ObjetoMuseoService.java` listados; `ExhibicionService.java`; `InventarioService.java:84-86`; `MovimientoInventarioService.java:25-27`; servicios de PDF/búsqueda.
- **Evidencia:** persisten `findAll()` y respuestas `List` sin paginación; búsquedas usan `%texto%`/`unaccent` sin índice trigram general; PDFs se materializan en memoria; se observó `PageImpl` directo.
- **Impacto:** latencia y heap crecientes con inventario real, potencial OOM en exportaciones.
- **Recomendación:** estimar volumen, paginar endpoints, limitar exportaciones y agregar índices según `EXPLAIN ANALYZE`.

### TLS-FINAL-001 — TLS/DNS real no fue validado

- **Severidad:** P2 — condición operativa.
- **Estado:** no verificable en este entorno.
- **Evidencia:** proxy y HTTPS fueron probados con nombres/certificado autofirmado sintéticos; no existe certificado institucional/Let's Encrypt ni DNS productivo en el repositorio, como corresponde.
- **Impacto:** cadena, SAN, renovación o hairpin DNS pueden fallar en el go-live.
- **Recomendación:** staging con DNS/certificado real, prueba de renovación y monitor de expiración.

### SUPPLY-FINAL-002 — No hay escaneo verificable de imágenes de contenedor

- **Severidad:** P2.
- **Componente:** Docker/supply chain.
- **Evidencia:** hay tags controlados (`postgres:16.11-alpine`, `nginx:1.27.4-alpine`, Node 22, Temurin 17, Keycloak 25.0.6), pero no pipeline Trivy/Grype/SBOM ni pin por digest.
- **Impacto:** CVEs del SO/base pueden quedar fuera de npm/Maven.
- **Recomendación:** generar SBOM y escanear las imágenes exactas del release; fijar política y digest tras validación.

## 6. Hallazgos P3

### API-FINAL-001 — Contratos de paginación no estables

- **Severidad:** P3.
- **Evidencia:** varios controllers serializan `PageImpl` directamente; Spring advierte que su JSON no es contrato estable.
- **Impacto:** upgrades pueden cambiar forma de respuesta.
- **Recomendación:** DTO paginado propio o `PagedModel`.

### API-FINAL-002 — Content-Disposition manual en descargas no fotográficas

- **Severidad:** P3.
- **Archivos/líneas:** `ObjetoMuseoController.java:406-412`; `ReciboIngresoObjetoController.java:57-64`; `VeteranoController.java:132-139`.
- **Evidencia:** algunas respuestas concatenan nombre original en el header, a diferencia del endpoint original que usa `ContentDisposition` con UTF-8.
- **Impacto:** nombres con comillas/no ASCII pueden producir header inválido o error, aunque el archivo físico sea UUID y no exista traversal.
- **Recomendación:** usar builder seguro en todas las descargas.

### DEV-FINAL-001 — Realm de desarrollo versiona credenciales conocidas

- **Severidad:** P3, condicionado a separación estricta.
- **Archivo:** `backend/docker/keycloak/museo-realm.json`.
- **Evidencia:** contiene tres usuarios dev con credenciales y un client secret conocido (valores redactados). Producción no importa este realm y el script productivo no crea usuarios, lo cual evita exposición directa.
- **Impacto:** riesgo si alguien reutiliza/importa por error el realm dev en un entorno accesible.
- **Recomendación:** rotularlo inequívocamente como dev, mantener control CI que prohíba su uso en compose prod y nunca reutilizar valores.

### OPS-FINAL-002 — Restore confía en el tar firmado por el mismo conjunto

- **Severidad:** P3.
- **Archivo/líneas:** `ops/restore.sh:37-69`.
- **Evidencia:** verifica SHA-256, pero no valida rutas del tar antes de extraer. Si un actor puede reemplazar archivo y manifest, podría introducir entradas inesperadas; el destino está contenido en el volumen aislado.
- **Impacto:** bajo bajo el modelo de backup protegido, pero endurecible.
- **Recomendación:** validar listado/rutas, propietario y tipos antes de extracción; proteger manifest con firma o almacenamiento inmutable.

## 7. Build y tests

| Comando | Resultado | Evidencia |
|---|---|---|
| `java -version` | OK | Temurin/OpenJDK 17.0.16 |
| `mvn -version` | OK | Maven 3.8.7 usando Java 17.0.16 |
| `mvn clean test` | OK | 313 ejecutados, 313 exitosos, 0 fallos, 0 errores, 0 omitidos; 2m07s |
| `mvn -Dtest=ExhibicionConcurrencyIntegrationTest test` | OK | 1 test Testcontainers/PostgreSQL 16, 0 fallos; el cuerpo repite la carrera cinco veces |
| `npm ci` | OK | 688 paquetes auditados |
| `npm audit --omit=dev` | OK runtime | 0 vulnerabilidades |
| `npm audit` | Con hallazgos dev | 22: 1 critical, 16 high, 3 moderate, 2 low |
| `npm run lint` | OK | sin errores |
| `npx tsc --noEmit` | OK | sin errores |
| `npm run build` | OK | Next.js 16.4.0; 39 páginas estáticas y rutas dinámicas |

Los tests verifican muchas invariantes, pero no cubren la baja del padre finalizado con devolución pendiente ni la carrera estado-de-exhibición vs asociación. Por ello el verde no es evidencia suficiente de go-live.

## 8. Seguridad

- **Verificado:** API anónima `401`; token de cliente sin `aud=museo-backend` `401`; token correcto `200`; VIEWER write `403`; MUSEOLOGO admin `403`; ADMIN admin `200`.
- **Verificado:** CORS prod se limita a `PUBLIC_APP_URL`; CSRF deshabilitado coherentemente con bearer stateless; Swagger deshabilitado en prod.
- **Verificado:** no se encontraron claves privadas, `.env` productivos, dumps ni backups versionados. Los valores productivos son placeholders/variables. El realm dev sí contiene secretos conocidos, redactados en esta auditoría.
- **Request ID:** regex `[A-Za-z0-9._-]{1,64}`; válido preservado; largo, Unicode y literal CR/LF reemplazados por UUID seguro. MDC se limpia en `finally`. En request autenticada real, MDC registró el subject y el proxy generó ID hexadecimal seguro.
- **Pendiente:** PROXY-FINAL-001/002, headers de navegador y escaneos supply chain.

## 9. Roles/permisos

Matriz reconstruida de `SecurityConfig`, anotaciones y servicios; “Sí limitado” indica reglas de visibilidad o endpoints específicos.

| Operación | ADMIN | MUSEOLOGO | VIEWER | Anónimo |
|---|---:|---:|---:|---:|
| Health/info | Sí | Sí | Sí | Sí |
| Leer objetos/exhibiciones/colecciones generales | Sí | Sí | Sí | No |
| Crear/editar/baja general | Sí | Sí | No | No |
| Activar “Permitir edición” frontend | Sí | Sí | No | No |
| Ver foto pública | Sí | Sí | Sí | No |
| Ver foto privada por endpoint común | Sí | Sí, watermark | No (`404`) | No |
| Descargar original | Sí | No (`403`) | No (`403`) | No (`401`) |
| Leer depositantes/ubicaciones/recibos/movimientos/pendientes | Sí | Sí | No | No |
| Administrar ubicaciones/catálogos restringidos | Sí | No | No | No |
| Eliminar colección | Sí | No | No | No |
| Embargos/comodatos admin | Sí | No | No | No |
| Administrar usuarios/roles/password | Sí | No | No | No |
| Verificar/revertir devolución | Sí | Sí | No | No |

No quedan referencias funcionales a OPERATOR: sólo documentación histórica/procedimiento y tests negativos. El realm productivo creado tuvo exactamente ADMIN/MUSEOLOGO/VIEWER. El backend prevaleció sobre la UI en pruebas directas.

## 10. Exhibiciones

- **Locking comprobado:** `ObjetoMuseoRepository.lockIdsForUpdate()` usa lock PostgreSQL; IDs ordenados; lock y validación están dentro de `@Transactional`; test real rechazó una de dos reservas y no dejó dos filas incompatibles.
- **Centralización parcial correcta:** creación principal, relación directa, edición y repetición llaman la autoridad de validación de asignaciones.
- **Finalización correcta en el flujo normal:** no inventa verificador/fecha; deja `PENDIENTE_REVISION`; verificación explícita registra actor, fecha y observaciones.
- **Cancelación previa:** libera semánticamente sin crear devolución física falsa.
- **Fallo bloqueante:** la baja lógica posterior del padre evita toda esa protección (EXH-FINAL-001).
- **Carrera residual:** el estado del padre no se revalida después del lock (EXH-FINAL-002).

## 11. Inventario

- `InventarioService.actualizar()` rechaza cambio de `objetoMuseoId` con conflicto.
- `MovimientoInventarioController` sólo expone GET; PUT/PATCH/DELETE no existen y devuelven 405/seguridad según ruta.
- Movimientos automáticos se agregan como nuevas filas con fecha y usuario resuelto desde contexto; la suite cubre alta/cambios.
- El historial se consulta ordenado por fecha en las rutas de objeto; no se halló mutación alternativa de movimientos.
- No existe operación explícita de “editar movimiento”; una corrección debe realizarse mediante la operación de negocio inversa/nuevo movimiento. Conviene documentar el procedimiento, pero no se encontró reescritura silenciosa.

## 12. Base de datos/Flyway

- V1–V28 se validaron/aplicaron desde vacío con PostgreSQL 16.11; Flyway quedó en versión 28.
- Perfil prod configura `ddl-auto=validate` y Flyway habilitado.
- El stack actual arrancó sobre el esquema V28 ya restaurado sin migraciones nuevas; no hay delta V29 que probar.
- Se inventariaron PK/FK/UNIQUE e índices. Relaciones con baja lógica reactivan fila o usan índices parciales donde corresponde; tests de Fase 1 permanecen verdes. Duplicados activos siguen bloqueados.
- `exhibicion_objeto` conserva UNIQUE global `(exhibicion_id,objeto_museo_id)` y reactiva la misma relación; colecciones/relaciones/recibos/embargos usan índices parciales activos cuando admiten historia múltiple.
- Riesgo principal: seed productivo V2 (DB-FINAL-001) y privilegios de DB (DB-FINAL-002).

## 13. Archivos/storage

- Imagen válida con filename `../../original.png` fue aceptada, almacenada bajo UUID y el DTO devolvió `nombreArchivoAlmacenado=null`.
- Contenido falso con extensión/MIME PNG → `400`; PNG real declarado JPEG → `400`; archivo de 11 MiB → `413`, no `500`.
- La validación común decodifica imagen, comprueba MIME/formato/dimensiones/píxeles; PDF verifica cabecera y EOF; límites negocio son 5/10 MiB y multipart global 10 MiB/51 MiB según flujo.
- Fotos, recibos y multimedia usan staging/commit/rollback y nombres físicos generados; reemplazo conserva anterior hasta commit. Tests específicos quedaron verdes.
- Foto privada: MUSEOLOGO endpoint común `200` con versión pública; VIEWER `404`; original ADMIN `200`, MUSEOLOGO/VIEWER `403`, anónimo `401`; `cmp` confirmó que bytes públicos no coinciden con original.
- Intentos directos a `/app/storage/...`, `/storage/...` y ruta relativa física respondieron `404`. Sólo backend monta storage.
- Reconciliador: dry-run por defecto; reporte `references=2, missing=0, orphans=0, unsafe=0`; un huérfano reciente no se marcó con edad mínima 24 h; cuarentena requiere flag explícito.

## 14. Frontend

- Lint, TypeScript y build productivo verdes.
- Keycloak JS usa Authorization Code + PKCE S256, token en memoria y refresh; no se encontraron tokens en localStorage.
- Guards y modo edición reconocen sólo ADMIN/MUSEOLOGO; VIEWER no habilita edición y backend bloquea acceso directo.
- Hallazgos: borrador sensible en localStorage (PRIV-FINAL-001), headers web incompletos y toolchain dentro del runner.
- El build local leyó `.env.local` ignorado con URLs localhost; no se usó como evidencia de producción. El build Docker productivo recibió args sintéticos HTTPS y arrancó correctamente.

## 15. Dependencias

### Frontend

- Runtime: `npm audit --omit=dev` = 0.
- Completo: 22 vulnerabilidades de dev/toolchain. La crítica es `proxy-addr <2.0.8`, transitiva de `shadcn`; `postcss` dev directo tiene fix no-major. No se demostró explotabilidad remota desde Next runtime.
- La copia de todo `node_modules` al runner convierte esto en hardening pendiente aunque npm las clasifique dev.

### Backend/IAM/containers

- `SUPPLY-001 NO VERIFICADO`: sin NVD key no hubo Dependency-Check válido.
- Revisión manual de versiones no sustituye scanner.
- Keycloak requiere upgrade/evaluación (IAM-FINAL-002).
- No hay scanner/SBOM de imágenes exactas.

## 16. Docker/infraestructura

Topología verificada:

```text
Internet/LAN
  -> Nginx :80/:443 (únicos puertos publicados)
     -> Next.js
     -> Spring Boot
     -> Keycloak
Spring Boot -> red interna -> PostgreSQL Museo
Keycloak    -> red interna -> PostgreSQL Keycloak
Spring Boot -> bind mount persistente de storage
```

- `docker compose ... config --quiet` = OK.
- Seis servicios healthy, restart `unless-stopped`, límites CPU/memoria/PID, filesystem read-only para app/proxy, tmpfs, logs `10m x 5`.
- DBs no publican 5432; sólo proxy publicó los puertos sintéticos loopback.
- Persistencia por bind mounts sobrevivió `docker compose down`/recreación: conteos `2 exhibiciones/1 foto/Flyway 28`, realm `200` y hash original idéntico.
- Reinicios aislados recuperaron: backend 34 s, frontend 7 s, Keycloak 29 s, PostgreSQL Museo 6 s; API autenticada y readiness volvieron a `200`.
- Bloqueos: archivos productivos aún no están en Git y usuarios DB son superusuarios.

## 17. Keycloak

- Modo productivo: `start --optimized`, no `start-dev`.
- PostgreSQL propio persistente, red interna y health de management `/health/ready`.
- Hostname estricto, proxy xforwarded, frontend público con standard flow/PKCE; direct grants deshabilitados en cliente real; admin client confidencial de service account.
- Script creó realm sin usuarios de aplicación, roles ADMIN/MUSEOLOGO/VIEWER y mapper `aud=museo-backend` sólo en access token.
- Wrong-audience real rechazado `401`; issuer/firma/expiración se validan con `JwtValidators.createDefaultWithIssuer` y Nimbus.
- Pendientes: versión 25.0.6, XFF de borde, atomicidad de rol y trazabilidad de administración.

## 18. TLS/proxy

- TLS 1.2/1.3, HSTS, tickets off, certificados montados read-only, HTTP→HTTPS y separación APP/AUTH.
- Proxy entrega Host, Proto, Port, Real-IP y Request-ID; client body 51 MiB alinea multipart global.
- HTTPS sintético funcionó; certificados privados no están versionados.
- Hallazgos: open redirect Host, XFF heredado, headers browser faltantes y TLS real no validado.

## 19. Backup/restore

### Backup independiente ejecutado

- `museo.dump`: 112.617 bytes.
- `keycloak.dump`: 220.196 bytes.
- `storage.tar.gz`: 772.019 bytes.
- Metadata y manifest SHA-256; `sha256sum -c` dio OK para los cuatro componentes.
- Backend y Keycloak se detuvieron durante la ventana; ambas DB y storage se capturaron coherentemente; servicios se reanudaron.
- Tiempo observado: 65 s.
- Metadata: app `ddd53ba...`, Flyway 28, PostgreSQL 16.11, sin secretos.

### Restore independiente ejecutado

- Destino: segundo proyecto Compose, DBs/directorios/puertos distintos.
- Checksums verificados antes de destruir el destino.
- Museo restaurado: 2 exhibiciones, 1 foto, Flyway 28.
- Keycloak restaurado: realm discovery `200` y health.
- Storage restaurado: archivo original presente y SHA-256 idéntico `22cbfd...`.
- Backend restaurado readiness `200`; los seis servicios healthy.
- Tiempo observado por script: 194 s (3m14s de pared incluyendo build/arranque).

Resultado: el mecanismo local es restaurable. No demuestra DR frente a pérdida del host porque falta copia off-host real.

RPO/RTO propuestos en documentación (24 h / 4 h) son técnicamente plausibles frente al restore de 194 s, pero requieren aprobación institucional, scheduler, capacidad de transferencia y simulacro desde medio externo.

## 20. Observabilidad

- Actuator separa liveness/readiness; Docker usa readiness para backend y health reales para DB/Keycloak/frontend/proxy.
- Logs rotan y contienen requestId, subject, método, endpoint, status y duración sin JWT completo.
- `health-check.sh` pasó: seis servicios, disco 77%, certificado sintético y backup age 0 h.
- Faltan programación/alerta real y señal de storage escribible; fallos de dependencia pueden colgar requests antes de responder 503.

## 21. Rendimiento

No se ejecutó benchmark por no existir carga/volumen representativo. El riesgo dominante es crecimiento no acotado: endpoints `List/findAll`, búsquedas `%texto%`, PDFs en memoria e imágenes de hasta 40 Mpx. Con dataset pequeño el smoke respondió; eso no valida volumen real. PERF-FINAL-001 debe medirse antes de fijar capacidad.

## 22. API

- 401/403 usan `ApiErrorResponse`; 400/404/409/413 están manejados centralmente; pruebas maliciosas devolvieron 400/413, no stack trace/500.
- Conflictos de disponibilidad se expresan 409 en flujos cubiertos.
- DTO de foto no expone ruta física.
- Pendientes: contrato `PageImpl`, algunas descargas con header manual y listados no paginados.

## 23. Documentación

Existen README, deployment backend, `PRODUCTION_DEPLOYMENT.md`, ejemplo de variables, procedimientos Keycloak/audience/OPERATOR→MUSEOLOGO, TLS, backup, restore, rollback forward-only y monitoreo. Java 17 está alineado en pom, Docker y documentación operativa; sólo informes históricos mencionan el antiguo contexto Java 21.

La documentación de Fase 4 no es entregable reproducible hasta versionarse (REL-FINAL-001). También debe incorporar las condiciones nuevas: no borrar exhibición con devolución pendiente, race residual, DB runtime least privilege, upgrade Keycloak y limpieza/segmentación de borradores.

## 24. Smoke/persistencia/resiliencia

| Prueba | Resultado |
|---|---|
| HTTPS frontend | `307 /dashboard`; página servida por Next detrás de Nginx |
| API anónima | `401` |
| JWT audiencia incorrecta | `401` |
| ADMIN/MUSEOLOGO/VIEWER lectura | `200/200/200` |
| VIEWER escritura | `403` |
| MUSEOLOGO admin | `403` |
| ADMIN admin | `200` |
| Foto privada/common/original | política correcta y bytes distintos |
| Upload falso/MIME spoof/oversize | `400/400/413` |
| Persistencia tras recrear containers | DB, realm y storage conservados |
| Reinicios | cuatro servicios recuperados |
| DB caída | readiness por proxy agotó 5 s; recuperó a `200` |
| Keycloak caído | login agotó 5 s; API con token/JWK cacheado siguió `200` |
| Backup/restore | exitoso; 65 s / 194 s |
| Bypass exhibición | **reproducido: 200 → 204 → 201** |

No se realizó login interactivo con navegador real ni smoke con certificado/DNS institucional. Los permisos backend se probaron con tokens reales emitidos por el Keycloak aislado.

## 25. Checklist final

- [x] Java 17 real y consistente.
- [x] Backend 313/313 verde.
- [x] Frontend lint, tipos y build verdes.
- [x] Runtime npm audit sin vulnerabilidades.
- [ ] Audit npm completo limpio; quedan dev/toolchain y se copian al runner.
- [ ] Dependency-Check backend válido.
- [x] Audience/issuer/firma/roles verificados.
- [x] Original sólo ADMIN; watermark/privacidad verificados.
- [x] Uploads falsos, MIME y tamaño rechazados.
- [x] Movimientos append-only e inventario inmutable por objeto.
- [x] Lock concurrente básico probado en PostgreSQL.
- [ ] Ninguna ruta evita invariantes de exhibición: **falla P0**.
- [ ] Auditoría completa de operaciones críticas.
- [x] Flyway V1–V28 desde vacío y `ddl-auto=validate`.
- [ ] Base nueva sin datos de desarrollo.
- [x] Compose productivo separado y sintaxis válida.
- [x] DB no publicada y storage persistente.
- [ ] Infraestructura incluida en commit/tag de release.
- [ ] Usuarios PostgreSQL de mínimo privilegio.
- [x] Backup ejecutable y checksum.
- [x] Restore aislado real exitoso.
- [ ] Copia off-host configurada/probada.
- [ ] TLS/DNS real y renovación probados.
- [ ] Monitoreo programado con alertas.
- [ ] Keycloak actualizado/evaluado por CVE.

## 26. Condiciones para go-live

1. Corregir EXH-FINAL-001 y agregar regresión que compruebe base final; repetir auditoría del flujo completo.
2. Cerrar EXH-FINAL-002 con lock/revalidación del padre y tests concurrentes estado-asignación.
3. Eliminar de producción los seeds V2 mediante migración/procedimiento forward-only validado sobre copia.
4. Versionar toda Fase 4, revisar commit final y construir imágenes desde tag/digest reproducible.
5. Ejecutar Dependency-Check válido y scan/SBOM de imágenes; resolver CRITICAL/HIGH aplicables; actualizar Keycloak.
6. Implementar auditoría mínima de usuarios/roles/passwords y archivos antes de uso real.
7. Separar roles DB owner/migrator/runtime y probar Flyway/despliegue.
8. Configurar copia externa cifrada, scheduler, retención, alerta y restaurar desde ese medio.
9. Corregir Host/XFF y aplicar headers web; validar certificado/DNS real.
10. Configurar health-check periódico/alertas, probar disco/storage y fijar RPO/RTO con la institución.
11. Mitigar borradores cross-user y revisar capacidad con volumen patrimonial representativo.

Después de cumplir estas condiciones debe repetirse una auditoría independiente sobre el commit/tag exacto candidato a producción.

## 27. Conclusión

**¿Pondría este sistema en producción en su estado actual? NO.**

La negativa no se basa en informes históricos: se reprodujo en el estado actual un bypass que libera administrativamente un objeto sin devolución verificada y permite reservarlo nuevamente. El backup/restore sí funciona en entorno aislado y varias garantías de seguridad están verificadas, pero no compensan una ruptura P0 de integridad patrimonial ni los pendientes P1 de auditoría, seeds, IAM, supply chain, release, privilegios y copia externa.

