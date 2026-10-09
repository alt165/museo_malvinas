# Fase 4 — Infraestructura productiva y recuperación

Fecha de validación: 2026-10-07  
Línea base: Fases 1, 2 y 3 completadas; Java 17; Flyway V1–V28.

## 1. Resumen

Se incorporó una topología productiva separada del entorno de desarrollo, con reverse proxy HTTPS, dos bases PostgreSQL independientes, Keycloak en modo productivo, servicios internos sin puertos publicados, almacenamiento persistente, healthchecks, reinicios, límites de recursos y rotación de logs.

También se implementaron y probaron scripts ejecutables de backup, restore, reconciliación de archivos, control operativo y análisis de dependencias backend. El backup y la restauración se validaron contra dos proyectos Docker aislados con datos sintéticos: se restauraron ambas bases y el storage, se levantó el stack restaurado y se verificó su integridad.

No se modificaron `docker-compose.yml`, las migraciones V1–V28 ni las reglas de aplicación de las fases anteriores. No se incorporaron secretos, certificados privados ni datos de prueba al repositorio.

## 2. Arquitectura productiva

```text
Internet / LAN institucional
        |
        | 80/443 (únicos puertos publicados)
        v
Reverse proxy Nginx + TLS
        |------------------------------|
        v                              v
Frontend Next.js                 Keycloak (modo prod)
        |                              |
        v                              v
Backend Spring Boot              PostgreSQL Keycloak
        |                         (red interna, usuario y DB propios)
        |------------------|
        v                  v
PostgreSQL Museo      Storage persistente
(red interna)         (originales, públicas, recibos y multimedia)

Backup consistente durante ventana de mantenimiento
        |
        v
Destino externo configurable + SHA-256 + retención
```

La definición productiva vive en `docker-compose.prod.yml`; el compose local continúa siendo exclusivamente de desarrollo.

## 3. Keycloak

- Imagen fijada: Keycloak `25.0.6`, construida de forma optimizada con PostgreSQL, health y metrics.
- Arranque: `start --optimized`; no se usa `start-dev` ni importación automática del realm.
- Persistencia: PostgreSQL 16.11 dedicado lógicamente, con base, usuario, contraseña, volumen y red propios. No comparte credenciales con Museo.
- Hostname: feature `hostname:v2`, hostname público estricto y proxy `xforwarded`. Se verificó arranque sin advertencias por opciones hostname obsoletas.
- Realm: `ops/keycloak/configure-realm.sh` configura idempotentemente `ADMIN`, `MUSEOLOGO` y `VIEWER`, sin `OPERATOR` ni usuarios de desarrollo.
- Audience: mapper de access token para `aud = museo-backend`.
- Clientes: backend bearer-only, frontend público con authorization code + PKCE y cliente administrativo confidencial con service account y permisos mínimos para administración de usuarios.
- Secrets: bootstrap admin y client secret se reciben únicamente por entorno; no están en la imagen ni en el realm versionado.

El procedimiento productivo documentado no modifica tablas internas de Keycloak. Para usuarios históricos con `OPERATOR`, se crea/asigna primero `MUSEOLOGO`, se verifica el token y recién entonces se retira `OPERATOR` mediante Admin Console o `kcadm.sh`. La importación de JSON no se considera un mecanismo de upgrade de un realm existente.

## 4. Docker

| Servicio | Red/puerto productivo | Persistencia | Healthcheck | Reinicio |
|---|---|---|---|---|
| reverse-proxy | publica 80/443 | certificados montados sólo lectura | endpoint local de health | `unless-stopped` |
| frontend | sólo red `edge` | no requiere datos | HTTP real sobre Node | `unless-stopped` |
| backend | `edge` + red interna Museo | storage por bind mount | Actuator readiness | `unless-stopped` |
| Keycloak | `edge` + red interna Keycloak | DB dedicada | management `/health/ready` | `unless-stopped` |
| PostgreSQL Museo | sólo red interna | directorio persistente | `pg_isready` | `unless-stopped` |
| PostgreSQL Keycloak | sólo red interna | directorio persistente | `pg_isready` | `unless-stopped` |

Sólo Nginx publica puertos. PostgreSQL, Keycloak, frontend y backend no quedan accesibles directamente desde el host. Las imágenes base tienen tags controlados (`postgres:16.11-alpine`, `nginx:1.27.4-alpine`, Keycloak 25.0.6 y los Dockerfiles Java/Node existentes).

Se configuraron límites iniciales revisables: backend 1536 MiB/2 CPU, Keycloak 1024 MiB/1.5 CPU, cada PostgreSQL 768 MiB/1 CPU, frontend 512 MiB/1 CPU y proxy 128 MiB/0.5 CPU. El backend usa 70% de la memoria del contenedor como máximo de heap para reservar memoria nativa durante el procesamiento de imágenes. Todos los servicios limitan PID y logs (`10m`, cinco archivos).

## 5. TLS / Reverse proxy

Nginx termina TLS 1.2/1.3 y enruta:

- `https://${APP_HOST}/` al frontend;
- `https://${APP_HOST}/api/` al backend;
- health público acotado al backend;
- `https://${AUTH_HOST}/` a Keycloak.

HTTP redirige a HTTPS. Se propagan `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host` y `X-Forwarded-Port`, sobrescribiendo los valores entrantes en el borde. Se agregaron HSTS y headers de seguridad básicos. Los certificados se montan desde un directorio externo e ignorado; ninguna clave privada se versiona.

La prueba aislada utilizó un certificado autofirmado sólo para verificar terminación TLS y routing. La instalación/renovación con certificado institucional o Let's Encrypt queda documentada y debe completarse con el DNS real.

## 6. Secrets

`.env.production.example` contiene sólo nombres y valores ficticios `CHANGE_ME`. `.env.production`, variantes locales, `secrets/` y `certs/` están ignorados por Git. Se separaron:

- contraseña de PostgreSQL Museo;
- contraseña de PostgreSQL Keycloak;
- bootstrap admin de Keycloak;
- secret del cliente administrativo;
- rutas externas de datos, certificados y backup.

El despliegue valida variables obligatorias antes de iniciar. El archivo real debe tener permisos 0600 y ser administrado fuera del repositorio. Docker secrets puede sustituir el archivo de entorno en una adaptación posterior del operador, sin cambiar la separación lógica diseñada.

## 7. Backup

`ops/backup.sh` implementa un conjunto consistente y automatizable:

1. valida que el destino sea absoluto y externo al directorio de datos;
2. detiene temporalmente backend y Keycloak para cerrar la ventana de escrituras;
3. ejecuta `pg_dump -Fc` de Museo y Keycloak por separado;
4. archiva todo el storage persistente;
5. genera metadata sin secretos (UTC, commit, Flyway, PostgreSQL, hostname y alcance);
6. genera `manifest.sha256`;
7. publica atómicamente el directorio, quitando el sufijo `.partial`;
8. reanuda los servicios aun si el backup falla.

Estructura verificada:

```text
backup-20261007T191912Z/
├── museo.dump
├── keycloak.dump
├── storage.tar.gz
├── metadata.txt
└── manifest.sha256
```

Retención inicial: 7 diarios, 4 semanales y 12 mensuales, configurable por entorno. Los enlaces de retención usan hard links cuando el filesystem lo permite y copia como fallback. El destino debe residir en NAS, disco desmontable o almacenamiento secundario; conservarlo sólo en el servidor no satisface la política.

## 8. Restore

`ops/restore.sh` requiere simultáneamente `--backup-dir` y `--confirm-restore`. Antes de tocar datos verifica el manifest; luego detiene escritores, recrea/restaura ambas bases, preserva el storage anterior como `storage.pre-restore-*`, restaura archivos y ejecuta comprobaciones de Flyway, tablas principales y realm. Finalmente levanta el stack completo con espera de healthchecks.

Prueba REAL ejecutada en dos proyectos Docker aislados:

- origen con PostgreSQL Museo, PostgreSQL Keycloak, Keycloak, backend, frontend y proxy;
- backup consistente generado desde el origen;
- `sha256sum -c manifest.sha256`: todos los archivos correctos;
- destino nuevo con directorios y contenedores independientes;
- restore de ambos dumps y storage;
- Flyway detectó versión/`installed_rank` 28;
- tablas principales accesibles y datos sintéticos esperados presentes;
- realm `museo` presente en Keycloak restaurado;
- hashes del storage origen/restaurado idénticos;
- los seis servicios del destino quedaron healthy;
- readiness del backend devolvió HTTP 200 y el archivo restaurado fue servido con HTTP 200.

Tiempo observado del script de restauración: **114 segundos** sobre un fixture pequeño en el host de prueba. No debe extrapolarse linealmente a la cantidad real de imágenes y datos.

## 9. RPO/RTO

- RPO inicial propuesto: **24 horas**, basado en un backup diario; para jornadas intensivas se recomienda aumentar la frecuencia.
- RTO inicial propuesto: **4 horas**, contemplando obtención del backup externo, validación, restauración, smoke test y decisión operativa.
- RTO técnico observado en fixture pequeño: **114 segundos** para el script completo.

Estos valores son objetivos operativos iniciales y requieren aprobación institucional y una prueba periódica con volumen representativo.

## 10. FILE-003

`ops/reconcile-storage.sh` compara todas las rutas persistidas en DB con los archivos físicos, incluyendo fotos original/pública, recibos escaneados, imágenes de personas y copias firmadas.

- El modo predeterminado es **dry run**.
- Genera reportes separados de referencias inexistentes, paths inseguros y archivos físicos sin referencia.
- Considera referencias de filas eliminadas lógicamente para preservar trazabilidad.
- Nunca elimina directamente.
- Sólo con `--quarantine-orphans` mueve huérfanos a `storage/quarantine/<timestamp>`.
- Excluye archivos recientes; la edad mínima predeterminada es 24 horas y no admite valores menores a una hora.

Prueba realizada con un huérfano sintético antiguo: el dry run lo detectó y lo conservó; la ejecución explícita lo movió a cuarentena. Resultado final: dos referencias válidas, cero referencias faltantes, un huérfano detectado y uno en cuarentena.

## 11. Observabilidad

`ops/health-check.sh` comprueba:

- estado healthy de los seis servicios;
- porcentaje de disco contra un umbral configurable;
- expiración del certificado TLS;
- antigüedad del último backup.

Actuator mantiene liveness separado de readiness; Docker usa readiness para declarar operativo al backend, sin transformar fallos transitorios de dependencias en una política agresiva de reinicio. Keycloak expone health/metrics por la interfaz de management sólo dentro de la topología. Los logs Docker están rotados para evitar crecimiento ilimitado.

El documento de despliegue define alertas mínimas para servicios, readiness, disco, backup, TLS, errores 5xx y fallos de login. No se agregó una plataforma de monitoreo compleja en esta fase.

## 12. SUPPLY-001

El pendiente quedó **preparado pero no cerrado con evidencia de CVE** porque el entorno no dispone de una `NVD_API_KEY` válida y no se inventó ninguna credencial.

- `ops/dependency-check.sh` usa OWASP Dependency-Check Maven 13.0.0, cache persistente y reportes HTML/JSON/SARIF.
- Exige la key por entorno y falla con CVSS >= 7.0 (HIGH/CRITICAL).
- `.github/workflows/backend-dependency-check.yml` lo ejecuta semanalmente o manualmente con `secrets.NVD_API_KEY` y conserva los reportes.
- El script fue probado sin key y terminó de forma controlada indicando la credencial faltante.

Hasta ejecutar ese workflow con una fuente NVD válida no puede afirmarse que el backend esté libre de vulnerabilidades conocidas.

## 13. Archivos creados/modificados

| Archivo | Motivo |
|---|---|
| `docker-compose.prod.yml` | Topología productiva separada |
| `.env.production.example` | Contrato de configuración sin secretos reales |
| `.gitignore` | Exclusión de secrets, certificados y datos operativos |
| `ops/nginx/default.conf.template` | HTTPS, routing y headers proxy |
| `ops/keycloak/Dockerfile` | Build optimizado Keycloak productivo |
| `ops/keycloak/configure-realm.sh` | Configuración idempotente del realm |
| `ops/lib.sh` | Funciones compartidas y carga segura de entorno |
| `ops/backup.sh` | Backup consistente, manifest y retención |
| `ops/restore.sh` | Restore destructivo explícito y verificado |
| `ops/reconcile-storage.sh` | Reconciliación dry-run/cuarentena |
| `ops/health-check.sh` | Control mínimo operativo |
| `ops/dependency-check.sh` | Escaneo Maven con NVD y umbral CVSS |
| `ops/dependency-check-suppressions.xml` | Archivo de supresiones explícitas inicialmente vacío |
| `.github/workflows/backend-dependency-check.yml` | Integración CI semanal/manual |
| `PRODUCTION_DEPLOYMENT.md` | Runbook de despliegue, migración, backup, restore y rollback |
| `README.md` | Enlace y separación explícita dev/prod |
| `PRE_PRODUCTION_FIX_PHASE_4.md` | Evidencia y decisiones de esta fase |

No se crearon migraciones Flyway ni se modificó código de dominio.

## 14. Tests ejecutados

| Comando/prueba | Resultado | Observaciones |
|---|---|---|
| `mvn clean test` | OK | 313 ejecutados, 313 exitosos, 0 fallos, 0 errores, 0 omitidos; Java 17 |
| `npm run lint` | OK | Sin errores |
| `npx tsc --noEmit` | OK | Sin errores de tipos |
| `npm run build` | OK | Build Next.js productiva; 39 rutas estáticas |
| `npm audit --omit=dev --json` | OK | 0 vulnerabilidades runtime |
| `docker compose -f docker-compose.prod.yml config --quiet` | OK | Configuración válida con entorno sintético |
| Stack productivo aislado `up --build --wait` | OK | Seis servicios healthy |
| `bash -n ops/*.sh ops/keycloak/*.sh` | OK | Sintaxis válida |
| Backup + `sha256sum -c` | OK | Museo, Keycloak, storage y metadata íntegros |
| Restore aislado completo | OK | Ambas DB, realm, Flyway, storage y servicios verificados |
| Reconciliación dry-run/cuarentena | OK | Sin borrado automático; edad mínima respetada |
| Dependency-Check sin NVD key | No ejecutable | Falla explícita prevista; CI queda preparada |

## 15. Smoke tests

Sobre el stack productivo aislado y con usuarios/clientes temporales no versionados se verificó:

- frontend detrás de TLS y redirect esperado;
- login y tokens Keycloak con `aud = museo-backend`;
- roles exactos `ADMIN`, `MUSEOLOGO` y `VIEWER`;
- consulta de objetos para los tres roles;
- escritura alcanza validación de negocio para ADMIN/MUSEOLOGO y devuelve 403 para VIEWER;
- administración de usuarios: ADMIN 200, MUSEOLOGO 403;
- carga de una imagen válida;
- versión común idéntica para VIEWER/MUSEOLOGO y distinta del original;
- descarga original: ADMIN 200, MUSEOLOGO/VIEWER 403;
- API anónima protegida: 401;
- readiness backend: 200;
- endpoint público del realm: 200.

## 16. Persistencia

Se creó información sintética y un archivo, se ejecutó `docker compose down` sin borrar volúmenes/directorios y se levantó nuevamente el stack. Después del reinicio:

- las filas de Museo continuaron presentes;
- el realm de Keycloak continuó presente;
- los archivos original/público continuaron en storage y fueron accesibles;
- todos los servicios recuperaron estado healthy.

La persistencia se basa en bind mounts bajo `MUSEO_DATA_DIR`, no en el filesystem efímero de los contenedores.

## 17. Riesgos pendientes

1. **SUPPLY-001 — Alta:** falta una ejecución real de Dependency-Check con `NVD_API_KEY`; la integración está lista, no la evidencia.
2. **Replicación de backup — Alta operativa:** se probó el backup en un destino separado del data root, pero el museo debe configurar y probar su copia física a NAS/disco/servidor secundario.
3. **TLS/DNS reales — Media:** el routing se probó con certificado sintético; falta instalar el certificado institucional, DNS y renovación/alerta reales.
4. **Sizing — Media:** los límites son una base conservadora; deben validarse con volumen real, en particular imágenes cercanas a 40 millones de píxeles y tamaño total del realm.
5. **Migración de realm existente — Media:** el procedimiento está documentado, pero debe ensayarse sobre una copia anonimizada del Keycloak real antes de producción.
6. **RTO real — Media:** el tiempo de 114 segundos corresponde a un fixture pequeño; debe medirse con el backup completo esperado.
7. **Operación de reconciliación — Baja:** el usuario operativo necesita permisos de lectura/escritura sobre storage para reportar y poner en cuarentena; no debe ejecutarse como una tarea automática de borrado.

Ninguno de estos riesgos se oculta como “resuelto”; deben formar parte de la auditoría independiente previa al go-live.

## 18. Checklist producción

- [x] Compose productivo separado del desarrollo.
- [x] Keycloak usa `start --optimized`, PostgreSQL persistente y realm sin usuarios de desarrollo.
- [x] Roles productivos y audience `museo-backend` configurables de forma soportada.
- [x] Secrets y certificados reales fuera de Git.
- [x] Sólo reverse proxy publica puertos; ambas DB permanecen internas.
- [x] Healthchecks principales y restart policies configurados.
- [x] Storage y ambas bases persisten recreación de contenedores.
- [x] Logs Docker limitados.
- [x] Backup ejecutable de ambas DB y storage, con manifest y retención.
- [x] Restore real aislado probado y verificado.
- [x] Reconciliador dry-run con cuarentena explícita y edad mínima.
- [x] Backend 313/313 y frontend lint/types/build verdes.
- [x] Java 17 continúa siendo la versión oficial.
- [ ] Configurar `NVD_API_KEY` y obtener un reporte backend válido.
- [ ] Configurar y probar réplica externa real de backups.
- [ ] Instalar y probar DNS/certificado/renovación productivos.
- [ ] Ensayar migración del realm existente con copia anonimizada.
- [ ] Aprobar institucionalmente RPO 24 h y RTO 4 h.
- [ ] Ejecutar una nueva auditoría completa e independiente antes del go-live.

## 19. Estado

Los criterios técnicos de esta fase se cumplieron: Keycloak no usa modo desarrollo, tiene persistencia separada; no se versionaron secretos; las DB no se publican; existen backup y restore ejecutables y el restore fue probado; los healthchecks principales están presentes; backend y frontend permanecen verdes.

`SUPPLY-001` conserva una limitación externa explícita por ausencia de credencial NVD, con pipeline preparado para cerrarla. Los pasos institucionales y de entorno real permanecen como condiciones previas al despliegue, no como garantías simuladas.

**FASE 4 COMPLETADA**

Este estado no declara al sistema listo para producción. Conforme al alcance solicitado, la decisión final requiere una nueva auditoría completa e independiente.
