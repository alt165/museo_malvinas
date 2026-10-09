# Fase 3 — Seguridad técnica y archivos

## 1. Resumen ejecutivo

La Fase 3 corrigió los hallazgos `DEP-001`, `FILE-001`, `FILE-002`, `SEC-002` y `LOG-001`, y formalizó Java 17 como versión única del proyecto. `SUPPLY-001` fue investigado con dos scanners, pero la auditoría de CVE backend continúa técnicamente inconclusa porque los servicios externos exigieron credenciales no disponibles; esta limitación queda documentada y no se interpreta como ausencia de vulnerabilidades.

Resultados finales verificados:

- Backend: **313/313 tests exitosos**, 0 fallos, 0 errores y 0 omitidos, ejecutados con Java 17.0.16.
- Flyway: **28/28 migraciones** aplicadas desde una base PostgreSQL 16 vacía mediante Testcontainers.
- Frontend: lint, TypeScript y build de producción exitosos con Next.js 16.4.0.
- `npm audit --omit=dev`: **0 vulnerabilidades runtime**.
- JWT: issuer, firma, tiempo y audiencia `museo-backend` validados.
- Uploads: validación por contenido real, nombres físicos generados, límites uniformes y compensación filesystem/DB.
- Request ID: sólo se acepta `[A-Za-z0-9._-]{1,64}`; cualquier otro valor se reemplaza por un UUID generado por el backend.
- No se modificaron migraciones Flyway ni se creó una nueva migración.

## 2. Estado inicial

### npm audit inicial

El `npm audit --omit=dev --json` ejecutado antes de actualizar el lockfile reportó **6 vulnerabilidades runtime: 1 crítica, 4 altas y 1 moderada**. El hallazgo directo principal era Next.js 16.2.5; también aparecían rutas transitivas relacionadas con `sharp`, `postcss`, `nanoid`, `source-map-js` y `baseline-browser-mapping`.

`package.json` utilizaba `latest` en la mayoría de dependencias directas, lo que impedía reproducir con precisión una instalación futura.

### Java detectado

- `java -version`: Eclipse Temurin OpenJDK **17.0.16**.
- `mvn -version`: Apache Maven **3.8.7**, ejecutándose con Java **17.0.16**.
- `pom.xml` ya definía `java.version=17`, pero faltaba formalizar `maven.compiler.release=17` y había documentación que permitía ambiguamente “17+”.

### Estado inicial de uploads

- Las fotografías de objetos decodificaban imágenes y generaban una versión pública, pero esa protección no estaba reutilizada por otros uploads.
- Recibos escaneados, copias firmadas y multimedia de personas confiaban principalmente en MIME/extensión declarados.
- Los nombres y mecanismos de persistencia no eran uniformes.
- Había ventanas donde un rollback de DB podía dejar archivos nuevos huérfanos, y reemplazos que podían eliminar el archivo anterior antes de confirmar el nuevo estado.
- La carga múltiple no tenía semántica atómica explícita.
- Spring no tenía límites multipart alineados con los límites de negocio.

### Scanner Maven disponible

No había scanner configurado en el proyecto. Se intentaron OWASP Dependency-Check 13.0.0 y Sonatype OSS Index 3.2.0 desde Maven. Los resultados y limitaciones se detallan en la sección 6.

## 3. DEP-001

Se reemplazaron todas las referencias `latest` por versiones explícitas. La actualización fue dirigida: Next.js y su configuración ESLint pasaron a 16.4.0, el lockfile resolvió versiones transitivas corregidas y se fijaron overrides sólo para dos transitivas que permanecían afectadas.

| Paquete | Vulnerabilidad/ruta informada inicialmente | Antes | Después | Acción |
|---|---|---:|---:|---|
| `next` | Advisories críticos/altos de Next.js informados por npm | 16.2.5 | 16.4.0 | Actualización menor directa |
| `sharp` | Ruta transitiva runtime | 0.34.5 | 0.35.5 | Actualizado por el nuevo árbol de Next.js |
| `nanoid` | Ruta transitiva runtime | 3.3.12 | 3.3.20 | Actualización transitiva |
| `source-map-js` | Ruta transitiva runtime | 1.2.1 | 1.2.2 | Override explícito mínimo |
| `baseline-browser-mapping` | Ruta transitiva runtime | 2.10.27 | 2.11.27 | Override explícito mínimo |
| `postcss` | Aparecía en el reporte inicial | 8.5.14 | 8.5.14 | El audit final ya no reporta advisory para el árbol instalado; no se forzó una actualización innecesaria |

No se ejecutó `npm audit fix --force` ni una actualización mayor indiscriminada.

Resultado final verificado de `npm audit --omit=dev`:

| Severidad | Cantidad |
|---|---:|
| Critical | 0 |
| High | 0 |
| Moderate | 0 |
| Low | 0 |
| Total | **0** |

El audit completo, incluyendo herramientas de desarrollo, todavía reporta 22 vulnerabilidades (1 crítica, 16 altas, 3 moderadas y 2 bajas). No forman parte del grafo runtime medido por el criterio de aceptación, pero deben tratarse en una actualización controlada del toolchain; no se ocultaron ni se aplicó `--force`.

## 4. FILE-001

Se creó `UploadFileValidator` como autoridad común. Para imágenes permite JPEG, PNG y WebP; comprueba tamaño, MIME declarado, formato detectado por `ImageIO`, decodificación completa, dimensiones y un máximo configurable de píxeles. El MIME detectado debe corresponder razonablemente con el declarado. Para PDF se verifican la firma `%PDF-` y un marcador final `%%EOF` en el tramo final del archivo.

Los nombres físicos se generan como UUID más extensión canónica. El nombre original se conserva únicamente como metadata cuando el modelo lo requiere. El storage normaliza y comprueba que las rutas permanezcan dentro de su raíz y escribe primero a un temporal, seguido de movimiento atómico cuando el filesystem lo soporta.

`TransactionalFileLifecycle` registra compensaciones transaccionales:

- archivo nuevo: se elimina en rollback;
- reemplazo: el anterior se conserva hasta `afterCommit`;
- si falla validación o persistencia, la metadata anterior y el archivo anterior permanecen válidos;
- un fallo de filesystem ocurre antes de persistir metadata nueva.

La carga múltiple de fotografías y multimedia de veteranos es ahora **atómica por request**: se validan todos los archivos antes de escribir; si cualquiera falla, ninguno se persiste. Se limita a 10 archivos por request.

| Tipo | Validación | Atomicidad | Resultado |
|---|---|---|---|
| Fotografías de objetos | Imagen real JPEG/PNG/WebP, MIME concordante, decode, 5 MB, píxeles | Lote completo atómico; cleanup en rollback | Protecciones previas y watermark conservados |
| Recibo escaneado del objeto | Imagen real o PDF con firma/EOF, 10 MB | Nuevo en rollback; anterior sólo se borra tras commit | Reemplazo seguro |
| Copia firmada del recibo de ingreso | Imagen real o PDF con firma/EOF, 10 MB | Temporal + movimiento; rollback/afterCommit | Reemplazo seguro y nombre UUID |
| Multimedia/imágenes de personas (veteranos) | Imagen real JPEG/PNG/WebP, MIME concordante, decode, 5 MB, píxeles | Lote completo atómico; cleanup en rollback | Ya no confía sólo en MIME/extensión |
| Otros uploads | No se encontraron otras familias multipart persistidas | No aplica | Sin cambios |

El límite de píxeles por defecto es 40.000.000 y es configurable con `APP_UPLOAD_MAX_IMAGE_PIXELS`. Permite originales fotográficos de alta resolución habituales (por ejemplo, 8.000 × 5.000) y acota la memoria necesaria para decodificarlos aun cuando el archivo comprimido sea pequeño.

## 5. FILE-002

Configuración final:

- `spring.servlet.multipart.max-file-size`: **10 MB** global técnico.
- `spring.servlet.multipart.max-request-size`: **51 MB** global técnico.
- fotografías/multimedia: **5 MB por archivo**.
- recibos: **10 MB por archivo**.
- carga múltiple: máximo **10 archivos**, con máximo teórico de negocio de 50 MB más overhead multipart.

El límite global permite las operaciones legítimas y los servicios mantienen límites de negocio más estrictos por tipo. `MaxUploadSizeExceededException` se traduce a HTTP **413 Payload Too Large** con el contrato `ApiErrorResponse`; no produce 500.

## 6. SUPPLY-001

### OWASP Dependency-Check

Comando intentado:

```text
mvn org.owasp:dependency-check-maven:13.0.0:check -Dformat=JSON -Dformat=HTML -DdataDirectory=/tmp/museo-dependency-check-data -DfailBuildOnCVSS=7
```

Resultado: **no concluyente**. El plugin no pudo obtener datos NVD y terminó con `Invalid API Key, length 0` / `NoDataException`. No se inventó ni incorporó una NVD API key y no se generó un reporte válido.

### Sonatype OSS Index

Comando intentado:

```text
mvn org.sonatype.ossindex.maven:ossindex-maven-plugin:3.2.0:audit
```

El plugin resolvió 192 artefactos, pero el servicio remoto respondió HTTP 401 Unauthorized. Aunque Maven mostró `BUILD SUCCESS`, no hubo resultados por componente y por lo tanto no constituye evidencia de seguridad.

No estaban instalados Trivy, Grype, OSV-Scanner, Snyk ni Syft. En consecuencia, **no se declara que el backend esté libre de vulnerabilidades**. Pendiente operativo: configurar en CI una NVD API key o credenciales/feed soportados, cache persistente y una política que bloquee CRITICAL/HIGH.

## 7. SEC-002

Se eligió el claim estándar `aud`. El backend requiere que la colección de audiencias contenga exactamente el client ID configurado, actualmente `museo-backend`.

`SecurityConfig` construye un `NimbusJwtDecoder` con el JWK Set configurado y combina:

- validaciones estándar de tiempo;
- issuer configurado;
- firma verificada por las claves del realm;
- `RequiredAudienceValidator` para `museo-backend`.

No se usó `azp` como sustituto de audience. El realm de desarrollo incorpora un mapper OIDC de audience a los clientes `museo-local` y `museo-frontend`, de modo que los access tokens destinados al backend contienen `aud: museo-backend`. Producción debe replicar ese mapper con mecanismos soportados por Keycloak antes de desplegar el backend endurecido.

Tests verificados: audience correcta, issuer incorrecto, audience incorrecta, ausencia de audience y conservación de roles/claims.

## 8. LOG-001

Formato permitido para `X-Request-Id`:

```text
[A-Za-z0-9._-]{1,64}
```

No se recorta ni normaliza un valor cliente: si es vacío, demasiado largo, contiene CR/LF, controles, espacios, Unicode u otro carácter fuera del patrón, se descarta completamente y se genera un UUID backend. El mismo valor seguro se utiliza en MDC y en el header de respuesta. El MDC se limpia en `finally`.

Se probaron UUID válido, alfanumérico válido, longitud excesiva, CR/LF, control, Unicode, vacío, ausencia, propagación y limpieza del MDC.

## 9. Java 17

Java 17 es la versión oficial y exacta de producción:

- Maven: `java.version=17` y `maven.compiler.release=17`.
- Compilación observada: `javac ... release 17`.
- Runtime de tests: Eclipse Temurin 17.0.16.
- Docker backend: build con Maven/Temurin 17 y runtime JRE Temurin 17; ya estaba alineado y se conservó.
- CI: el repositorio no contiene pipeline CI para ajustar; queda como requisito que el futuro runner use JDK 17.
- Documentación operativa (`README`, `AGENTS`, `SETUP`, `SECURITY`, `DEPLOYMENT`) alineada con Java 17.
- No quedan referencias operativas a Java/JDK/Temurin 21. Las únicas menciones restantes están en informes históricos que describen el contexto de auditoría y no se reescribieron.

## 10. Archivos modificados

### Raíz y documentación

- `README.md`: backend documentado sobre Java 17.
- `PRE_PRODUCTION_FIX_PHASE_3.md`: este reporte.
- `backend/AGENTS.md`: Java 17 exacto, sin APIs posteriores.
- `backend/DEPLOYMENT.md`: runtime Java 17 y requisito del mapper de audience.
- `backend/SECURITY.md`: audience JWT y política de Request ID.
- `backend/SETUP.md`: variables/límites de uploads y semántica atómica.

### Backend y Keycloak

- `backend/pom.xml`: compiler release 17.
- `backend/docker/keycloak/museo-realm.json`: mappers de audience `museo-backend`.
- `backend/src/main/resources/application.yml`: límites multipart, cantidad de archivos y píxeles.
- `RequestTracingFilter.java`: validación/regeneración de request ID.
- `SecurityConfig.java`: decoder con validación combinada.
- `RequiredAudienceValidator.java`: validador explícito de `aud`.
- `GlobalExceptionHandler.java`: respuesta 413 controlada.
- `UploadFileValidator.java`: validación común por contenido.
- `TransactionalFileLifecycle.java`: compensaciones según resultado transaccional.
- `ObjectFileStorageService.java`: nombres UUID, temporal y movimiento seguro.
- `FotoObjetoMuseoService.java` y `ObjetoMuseoController.java`: validación común y lote atómico.
- `VeteranoImagenService.java` y `VeteranoController.java`: validación común y lote atómico.
- `ReciboEscaneadoObjetoMuseoService.java`: validación y reemplazo transaccional.
- `ReciboIngresoObjetoService.java`: copia firmada validada y reemplazo transaccional.

### Tests backend

- `RequestTracingFilterTest.java`.
- `GlobalExceptionHandlerUploadTest.java`.
- `JwtTokenValidatorTest.java`.
- `UploadFileValidatorTest.java`.
- `ReciboIngresoObjetoServiceFileTest.java`.
- `ObjectFileStorageServiceTest.java`.
- `FotoObjetoMuseoServiceTest.java`.
- `ObjetoArchivoServiceIntegrationTest.java`.
- `VeteranoMultimediaIntegrationTest.java`.

### Frontend

- `frontend/package.json`: versiones explícitas, Next.js 16.4.0 y overrides dirigidos.
- `frontend/package-lock.json`: resolución reproducible actualizada.
- `frontend/next-env.d.ts`: referencia generada requerida por Next.js 16.4.0.

## 11. Dependencias modificadas

El cambio funcional de seguridad directo fue `next` 16.2.5 → 16.4.0 junto con `eslint-config-next` 16.4.0. El lockfile actualizó `sharp` 0.34.5 → 0.35.5 y `nanoid` 3.3.12 → 3.3.20. Se fijaron `source-map-js` 1.2.2 y `baseline-browser-mapping` 2.11.27 mediante overrides.

Las demás dependencias directas que usaban `latest` quedaron fijadas a la versión que estaba resuelta durante esta fase. No se cambió Spring Boot ni se agregó una dependencia runtime backend.

## 12. Tests agregados

Se agregaron o ampliaron 33 casos respecto de la línea base de 280:

- contenido falso con extensión válida;
- imagen corrupta;
- MIME declarado discordante;
- imagen válida y exceso de píxeles/tamaño;
- PDF con firma/terminación válida e inválida;
- filename malicioso/path traversal;
- reemplazo exitoso, reemplazo inválido y rollback DB;
- fallo de storage sin metadata persistida;
- atomicidad de lotes de fotografías y multimedia;
- recibos, fotos y multimedia de personas;
- HTTP 413 estable;
- issuer/audience JWT y preservación de roles;
- matriz de entradas seguras/inseguras de Request ID.

## 13. Tests ejecutados

| Comando | Resultado | Observaciones |
|---|---|---|
| `java -version` | OK | Temurin 17.0.16 |
| `mvn -version` | OK | Maven 3.8.7 sobre Java 17.0.16 |
| `mvn clean test` | OK | **313 tests**, 313 exitosos, 0 fallos, 0 errores, 0 omitidos |
| Testcontainers PostgreSQL/Flyway dentro de `mvn clean test` | OK | PostgreSQL 16; esquema vacío; V1–V28 validadas y aplicadas |
| `npm install` | OK | Lockfile actualizado sin `--force` |
| `npm audit --omit=dev --json` | OK | **0 vulnerabilidades runtime** |
| `npm run lint` | OK | ESLint sin errores |
| `npx tsc --noEmit` | OK | TypeScript sin errores |
| `npm run build` | OK | Next.js 16.4.0; 39 páginas estáticas más rutas dinámicas |
| OWASP Dependency-Check 13.0.0 | No concluyente | NVD rechazó consulta sin API key; sin reporte válido |
| Sonatype OSS Index 3.2.0 | No concluyente | 192 artefactos resueltos; API respondió 401; sin resultados válidos |

## 14. Regresiones

La suite completa conserva las garantías previas:

- SEC-001: sólo ADMIN descarga originales; MUSEOLOGO recibe la versión pública y VIEWER no accede a fotos privadas.
- Roles definitivos ADMIN/MUSEOLOGO/VIEWER y administración de usuarios.
- Soft delete/reactivación y duplicados activos.
- Lock real contra doble reserva concurrente de exhibiciones.
- Todas las asociaciones pasan por las invariantes centralizadas.
- Finalización no equivale a devolución/verificación.
- Inventario no cambia de objeto y movimientos son append-only.
- Flyway conserva V1–V28 sin alteraciones.

## 15. Hallazgos nuevos

1. **Toolchain frontend (media, fuera del runtime):** el audit sin `--omit=dev` reporta 22 vulnerabilidades. Requiere una fase controlada de actualización de herramientas y comprobación de build; no afecta el resultado runtime solicitado, pero debe incorporarse al mantenimiento.
2. **Dependencias backend sin inventario CVE verificable (alta, `SUPPLY-001` residual):** ambos servicios públicos probados exigen credenciales. Debe resolverse en CI con secretos y cache, no desde el código de aplicación.
3. **Warning de serialización `PageImpl` (ya relacionado con `API-001`):** sigue apareciendo durante tests y queda fuera de alcance de esta fase.
4. **Warning de `commons-logging` durante tests:** se observó una coexistencia potencial con `spring-jcl`; no causó fallos y debe investigarse junto con el análisis backend de dependencias.

## 16. Riesgos pendientes

- `SUPPLY-001` no puede cerrarse con evidencia de CVE backend hasta configurar un scanner autenticado y reproducible.
- Falta la infraestructura productiva definitiva: secrets, TLS/reverse proxy, persistencia/backup/restore, healthchecks completos y monitoreo siguen fuera de Fase 3.
- El mapper de audience debe replicarse y verificarse en el realm productivo antes de activar el backend; de otro modo los tokens serán rechazados correctamente.
- El límite de 40 millones de píxeles es configurable y debe confirmarse contra las cámaras y memoria disponibles en producción; la validación evita cargas ilimitadas, pero no sustituye límites de contenedor/heap.
- La compensación filesystem/DB cubre las operaciones de aplicación y sus rollbacks, pero un corte abrupto del proceso entre filesystem y commit todavía requiere el reconciliador/política de purga contemplado por `FILE-003`.

## 17. Estado

Los criterios funcionales y de regresión de Fase 3 están cumplidos. La excepción permitida por el criterio `SUPPLY-001` queda expresamente documentada: la auditoría backend no fue técnicamente posible sin credenciales válidas y no se declara falsa seguridad.

`FASE 3 COMPLETADA`
