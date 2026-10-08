# Despliegue productivo — Sistema Museo

Este procedimiento usa `docker-compose.prod.yml`. El `docker-compose.yml` raíz continúa siendo exclusivamente de desarrollo.

## 1. Requisitos del servidor

- Linux x86_64 con Docker Engine y Docker Compose v2.
- 4 vCPU y 8 GB RAM recomendados como punto de partida.
- Disco con al menos tres veces el tamaño combinado esperado de DB y storage.
- Java no es necesario en el host; las imágenes backend usan Java 17.
- `bash`, `curl`, `jq`, `openssl`, `tar`, `sha256sum`, `find` y `realpath` para scripts operativos.
- Un destino de backup montado desde NAS/disco externo/servidor secundario. El mismo disco no satisface recuperación ante desastre.

Recursos configurados inicialmente:

| Servicio | Memoria | CPU | Notas |
|---|---:|---:|---|
| Backend | 1536 MiB | 2 | Heap al 70%; margen para imágenes de hasta 40 MP |
| Keycloak | 1024 MiB | 1.5 | Perfil productivo optimizado |
| PostgreSQL Museo | 768 MiB | 1 | Ajustar tras medir volumen real |
| PostgreSQL Keycloak | 768 MiB | 1 | DB y usuario separados |
| Frontend | 512 MiB | 1 | Node limitado a 384 MiB |
| Nginx | 128 MiB | 0.5 | Único servicio publicado |

Son límites iniciales, no sizing definitivo. Vigilar OOM, heap, latencia y tamaño de imágenes antes de reducirlos.

## 2. DNS y red

Crear dos registros DNS hacia el servidor:

- `APP_HOST`: aplicación, por ejemplo `museo.institucion.gob.ar`.
- `AUTH_HOST`: identidad, por ejemplo `auth.museo.institucion.gob.ar`.

Abrir únicamente TCP 80/443. PostgreSQL, backend, frontend y Keycloak no publican puertos. Las redes `museo-db` y `keycloak-db` son internas y distintas; cada DB tiene usuario, contraseña y base propios.

## 3. Directorios, variables y secretos

```bash
sudo install -d -m 0750 /srv/museo/data /srv/museo/tls
sudo install -d -m 0750 /mnt/museo-backups
cp .env.production.example /ruta/segura/.env.production
chmod 600 /ruta/segura/.env.production
```

Reemplazar todos los `CHANGE_ME`. Generar secretos independientes, por ejemplo con `openssl rand -base64 48`. No reutilizar contraseñas ni el secreto de desarrollo. El archivo real debe permanecer fuera del repositorio y respaldarse mediante el gestor seguro institucional.

Secretos requeridos: contraseñas de ambas DB, administrador bootstrap Keycloak, secreto de `museo-admin` y credenciales del destino externo de backups. El acceso al socket Docker y a `docker inspect` se considera privilegiado porque Compose inyecta variables al proceso. Si la institución exige Docker Secrets/Vault, adaptar los entrypoints antes del despliegue, sin versionar valores.

## 4. TLS

`TLS_CERT_DIR` debe contener `fullchain.pem` y `privkey.pem`. La clave privada nunca se versiona y debe tener permisos mínimos. El certificado debe cubrir ambos hosts (SAN) o se debe adaptar la plantilla para certificados separados.

Puede usarse un certificado institucional o Let's Encrypt gestionado en el host/automatización externa. Después de renovar:

```bash
docker compose --env-file /ruta/segura/.env.production -f docker-compose.prod.yml exec reverse-proxy nginx -t
docker compose --env-file /ruta/segura/.env.production -f docker-compose.prod.yml exec reverse-proxy nginx -s reload
```

`ops/health-check.sh` falla cuando el certificado vence dentro de `TLS_WARN_DAYS` (30 por defecto).

## 5. PostgreSQL: instalación nueva y conversión de volúmenes existentes

En instalaciones nuevas, los scripts de `/docker-entrypoint-initdb.d` crean automáticamente:

- Museo: `museo_migrator` como owner/usuario Flyway y `museo_app` como runtime DML.
- Keycloak: `keycloak_app` como owner y usuario de migraciones internas.

Ninguno de esos usuarios runtime posee `SUPERUSER`, `CREATEDB`, `CREATEROLE` ni `BYPASSRLS`. Los usuarios `*_bootstrap` son exclusivamente administrativos, no se configuran en backend ni Keycloak.

Un volumen anterior no ejecuta de nuevo los scripts de init. Antes de usar este Compose sobre una instalación existente, mantener detenidas las aplicaciones, hacer backup y ejecutar con la definición/volumen anteriores:

```bash
# Variables temporales, sólo para esta conversión coordinada:
MUSEO_DB_LEGACY_ADMIN_USER=museo_app
MUSEO_DB_LEGACY_OWNER=museo_app
KEYCLOAK_DB_LEGACY_ADMIN_USER=keycloak_app

ENV_FILE=/ruta/segura/.env.production ./ops/postgres/configure-existing-museo-roles.sh
ENV_FILE=/ruta/segura/.env.production ./ops/postgres/configure-existing-keycloak-role.sh
```

Los nombres legacy deben ser los comprobados en esa instalación; no copiarlos a ciegas. El primer script reasigna únicamente objetos del owner explicitado al migrador y luego demueve el runtime. El segundo crea el bootstrap nuevo y demueve el owner Keycloak conservando su capacidad DDL. Comprobar `rolsuper = false` para ambos runtime antes de continuar. Retirar las tres variables legacy del archivo operativo tras la conversión.

Flyway productivo tiene `baseline-on-migrate=false`: una base no vacía sin historial válido debe fallar en vez de omitir silenciosamente migraciones. V29 retira el seed demo histórico sólo cuando reconoce el grafo V2 completo e intacto; si fue modificado o utilizado, lo preserva y emite una advertencia para revisión humana.

## 6. Primera instalación de Keycloak

Keycloak 26.8.0 se construye optimizado y ejecuta `start --optimized`; no usa `start-dev`. Su PostgreSQL vive en `${MUSEO_DATA_DIR}/postgres-keycloak` y no se comparte con Museo. El backend usa el Admin Client Java 26.0.12, cuya numeración es independiente de la del servidor.

```bash
docker compose --env-file /ruta/segura/.env.production -f docker-compose.prod.yml up -d --build --wait \
  postgres-museo postgres-keycloak keycloak

docker compose --env-file /ruta/segura/.env.production -f docker-compose.prod.yml exec -T keycloak \
  /opt/keycloak/tools/configure-realm.sh
```

El provisionador idempotente crea/configura roles `ADMIN`, `MUSEOLOGO`, `VIEWER`; clientes backend, frontend PKCE y administrativo; audience `museo-backend`; redirect URI/web origin exactos y permisos mínimos del service account. No crea usuarios de aplicación ni importa contraseñas de desarrollo. Proteger/rotar la cuenta bootstrap conforme a la política institucional y conservar una cuenta administrativa de emergencia auditada.

### Realm existente y migración OPERATOR → MUSEOLOGO

No importar `museo-realm.json` sobre un realm existente y no modificar tablas Keycloak.

1. Ejecutar un backup verificado de Keycloak.
2. Exportar mediante Admin Console/Admin REST los usuarios asignados a `OPERATOR`.
3. Crear `MUSEOLOGO` si falta y asignarlo a cada usuario, manteniendo temporalmente `OPERATOR`.
4. Crear/verificar el mapper OIDC de audience `museo-backend` en el cliente frontend.
5. Configurar redirect URIs/web origins exactos.
6. Rotar el secreto de `museo-admin`, actualizar el secreto externo y reiniciar backend.
7. Forzar nuevo login y comprobar `aud=museo-backend` y el rol correcto.
8. Verificar MUSEOLOGO, denegación de administración/originales y VIEWER sólo lectura.
9. Retirar `OPERATOR`; cuando la consulta soportada de asignaciones devuelva cero, eliminar el rol.

La coexistencia sólo se admite durante esa ventana coordinada; el backend no otorga permisos a `OPERATOR`.

### Upgrade Keycloak 25.0.6 → 26.8.0

No probar el upgrade sólo importando un realm nuevo. En una ventana de mantenimiento:

1. detener todos los nodos Keycloak y backend;
2. generar backup consistente de DB Keycloak y conservar la imagen 25.0.6;
3. restaurar ese dump en una DB/host aislado;
4. levantar **una sola** instancia 26.8.0 sobre la copia para que ejecute las migraciones soportadas;
5. verificar health, realm, roles, clientes, mapper audience, PKCE, Admin API, service account, login y refresh de los tres roles;
6. comprobar token de audiencia correcta y rechazo de audiencia incorrecta;
7. sólo entonces repetir el procedimiento controlado sobre producción.

La migración de esquema Keycloak es forward-only a efectos operativos: una vez migrada la DB no arrancar 25.0.6 sobre ella. El rollback seguro restaura el dump anterior completo y vuelve a la imagen anterior. En Fase 5 se ensayó esta ruta sobre una copia real de una DB 25.0.6; 26.8.0 aplicó su cadena de migraciones y preservó realm, roles y cliente.

## 7. Levantar y verificar el stack

```bash
docker compose --env-file /ruta/segura/.env.production -f docker-compose.prod.yml up -d --build --wait
docker compose --env-file /ruta/segura/.env.production -f docker-compose.prod.yml ps
ENV_FILE=/ruta/segura/.env.production ./ops/health-check.sh
```

Healthchecks: `pg_isready` para ambas DB, readiness Keycloak en management 9000, Actuator readiness backend, request HTTP interno frontend y `/healthz` del proxy. El backend conserva liveness separada. Un `unhealthy` no reinicia por sí solo un proceso vivo; `restart: unless-stopped` actúa ante salida del proceso y evita loops por una dependencia transitoriamente caída.

## 8. Smoke test posterior al despliegue

Usar identidades de prueba aprobadas y eliminarlas después:

1. frontend abre por HTTPS;
2. discovery/login Keycloak funciona;
3. access token contiene `aud=museo-backend`;
4. ADMIN, MUSEOLOGO y VIEWER consultan objetos;
5. MUSEOLOGO edita pero no administra;
6. VIEWER recibe 403 en mutaciones;
7. foto pública visible según política;
8. endpoint común devuelve versión pública a MUSEOLOGO/VIEWER;
9. sólo ADMIN descarga `/original`;
10. Actuator readiness responde 200 a través del proxy.

## 9. Backup automático

`ops/backup.sh` detiene brevemente backend y Keycloak, dejando las DB activas, y genera:

```text
backup-YYYYMMDDTHHMMSSZ/
  museo.dump
  keycloak.dump
  storage.tar.gz
  metadata.txt
  manifest.sha256
```

Los archivos privados se leen mediante un contenedor efímero con el volumen montado; no se relajan permisos. Ante error se elimina el staging parcial y se reanudan servicios.

Programación inicial recomendada: diariamente a las 02:00, monitorizando exit code.

```text
0 2 * * * cd /opt/museo && ENV_FILE=/ruta/segura/.env.production ./ops/backup.sh >>/var/log/museo-backup.log 2>&1
```

Retención: 7 diarios, 4 semanales (domingo) y 12 mensuales (día 1). Se usan hardlinks si el filesystem lo permite. La copia local no satisface disaster recovery. Replicar cada set terminado con uno de estos transportes:

```bash
# NAS/disco realmente independiente montado en el host
BACKUP_EXTERNAL_MODE=mounted \
BACKUP_EXTERNAL_PATH=/mnt/nas-independiente/museo \
ENV_FILE=/ruta/segura/.env.production \
./ops/replicate-backup.sh /mnt/museo-backups/daily/backup-YYYYMMDDTHHMMSSZ

# Servidor secundario por SSH; clave y known_hosts quedan fuera de Git
BACKUP_EXTERNAL_MODE=rsync-ssh \
BACKUP_EXTERNAL_SSH_TARGET=backup@servidor:/srv/backups/museo \
BACKUP_EXTERNAL_SSH_KEY=/run/secrets/museo_backup_ssh_key \
BACKUP_EXTERNAL_KNOWN_HOSTS=/etc/ssh/ssh_known_hosts \
ENV_FILE=/ruta/segura/.env.production \
./ops/replicate-backup.sh /mnt/museo-backups/daily/backup-YYYYMMDDTHHMMSSZ
```

El modo montado rechaza un destino en el mismo filesystem. Programar y monitorizar la réplica inmediatamente después del backup; verificar manifest remoto y ensayar periódicamente un restore descargando **desde esa copia externa**. Sin NAS/servidor real configurado, `OPS-FINAL-001` continúa pendiente operativo.

La ventana de mantenimiento asegura coherencia DB/filesystem: bloquea uploads, cambios de aplicación e identidad mientras se obtienen dumps y tar.

## 10. Restauración

Nunca ensayar sobre producción. Crear otro host/proyecto y un `MUSEO_DATA_DIR` vacío.

```bash
ENV_FILE=/ruta/aislada/.env.production ./ops/restore.sh \
  --backup-dir /mnt/museo-backups/daily/backup-YYYYMMDDTHHMMSSZ \
  --confirm-restore
```

El script valida SHA-256, restaura ambas DB con `pg_restore --exit-on-error`, preserva el storage previo como `storage.pre-restore-*`, extrae el backup, verifica Flyway/objetos/realm y espera todos los healthchecks. Después repetir smoke test, comparar conteos, descargar archivos de muestra y conservar el acta.

## 11. RPO y RTO iniciales

- **RPO propuesto: 24 horas**, por backup diario. Ejecutar uno adicional tras jornadas de carga intensiva.
- **RTO propuesto: 4 horas**, incluyendo host, transferencia, restore y validación humana.

Son objetivos para aprobación institucional, no SLA. El ensayo independiente de Fase 5 restauró el conjunto en 170 segundos una vez disponibles host, imágenes y backup; el volumen real y la transferencia externa dominarán el RTO real.

## 12. Reconciliación de archivos

Dry-run semanal:

```bash
sudo ENV_FILE=/ruta/segura/.env.production ./ops/reconcile-storage.sh --report-dir /var/log/museo-reconcile/fecha
```

Detecta archivos físicos sin referencia, referencias faltantes y rutas inseguras. Incluye filas dadas de baja porque sus binarios se conservan. Ignora cuarentena y archivos menores a `RECONCILE_MIN_AGE_HOURS` (24 por defecto).

Tras revisar DB, backup y reporte, la única acción es cuarentena explícita:

```bash
sudo ENV_FILE=/ruta/segura/.env.production ./ops/reconcile-storage.sh \
  --quarantine-orphans --report-dir /var/log/museo-reconcile/fecha-aplicada
```

No hay borrado definitivo automático. Definir retención/aprobación antes de vaciar `storage/quarantine`. El usuario que ejecuta el script debe poder leer y mover el storage; se recomienda la cuenta operativa privilegiada usada para backups.

## 13. Observabilidad mínima

Ejecutar `ops/health-check.sh` cada 5 minutos desde el monitor institucional. Devuelve no-cero por servicio no saludable, disco alto, certificado próximo a vencer o backup antiguo/ausente.

Alertar sobre exit code/replicación de backup, restarts/OOMKilled, espacio e inodos, expiración TLS, readiness/liveness, 5xx, login y latencia. Todos los contenedores usan `json-file` con 10 MB × 5 archivos. Centralizar `docker logs` si existe syslog/Loki/ELK institucional; no se agregó una plataforma nueva.

## 14. Auditoría de dependencias backend

Configurar el secret CI `NVD_API_KEY`. `.github/workflows/backend-dependency-check.yml` ejecuta semanal/manual `ops/dependency-check.sh`, conserva cache, publica reportes y falla con CVSS ≥ 7.0. Las supresiones requieren CVE y justificación revisada. En el entorno de Fase 5 la clave no estaba disponible: `SUPPLY-FINAL-001` continúa pendiente externo y no se afirma ausencia de CVE backend.

## 15. Rollback

- No editar ni revertir migraciones Flyway aplicadas.
- Sin migración incompatible: conservar DB/storage, desplegar imagen anterior compatible y repetir health/smoke.
- Con migración destructiva/corrupción: detener escrituras y restaurar el conjunto completo anterior (Museo + Keycloak + storage), no una DB aislada.
- Conservar imágenes previas identificadas y el backup previo a cada release.
- Registrar decisión, timestamps, versión, causa y verificaciones posteriores.
