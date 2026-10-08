# Fase 6 — Remediación de vulnerabilidades del backend

Fecha de verificación: 2026-10-08
Commit base (sin incluir los cambios de esta fase): `9bb3ea3e0fabb3f9c9bdbe3bccad765feff87caf`
Java objetivo y runtime: **17**

## 1. Alcance y resultado

Se actualizó de forma coordinada Spring Boot y su BOM, con excepciones de versión puntuales sólo donde el parche de seguridad publicado era posterior al BOM. No se modificaron reglas de negocio, contratos REST, código Java ni migraciones V1–V30.

Resultado técnico:

- compilación: **BUILD SUCCESS**;
- tests: **325 ejecutados, 0 fallos, 0 errores, 0 omitidos**;
- Dependency-Check 13.0.0: **BUILD SUCCESS**, `failBuildOnCVSS=7.0` sin reducir;
- vulnerabilidades críticas aplicables pendientes: **0**;
- vulnerabilidades altas aplicables pendientes: **0**;
- Docker productivo aislado: **6/6 servicios healthy**;
- Flyway: **V30 aplicada** sobre PostgreSQL 16.11;
- Keycloak: **26.8.0**, persistente en PostgreSQL y healthy.

## 2. Dependencias anteriores y finales

Las versiones son las efectivas observadas con `mvn dependency:tree`, no sólo las declaradas.

| Componente | Anterior | Final | Administración |
|---|---:|---:|---|
| Spring Boot | 3.3.5 | **3.5.16** | parent/BOM |
| Spring Framework | 6.1.14 | **6.2.19** | BOM de Boot |
| Spring Security | 6.3.4 | **6.5.11** | BOM de Boot |
| Spring Data JPA | rama Boot 3.3 | **3.5.13** | BOM de Boot |
| Tomcat Embed | 10.1.31 | **10.1.60** | override de parche |
| Jackson Databind/BOM | 2.17.2 | **2.21.7** | override de parche |
| PostgreSQL JDBC | 42.7.4 | **42.7.14** | override de parche |
| Log4j API/bridge | 2.23.1 | **2.26.1** | override de parche |
| Angus Mail | 2.0.3 | **2.0.5** | BOM de Boot |
| Springdoc OpenAPI | 2.6.0 | **2.9.1** | versión directa |
| Swagger UI | 5.17.14 | **5.32.14** | transitiva de Springdoc |
| Flyway | 10.10.0 | **11.7.2** | BOM de Boot |
| Keycloak Admin Client | 26.0.12 | **26.0.12** | sin cambio |

Spring Boot 3.5.16 conserva baseline Java 17. El compilador usó `release 17`; la imagen final ejecutó Temurin `17.0.20.1`.

### Excepciones al BOM

- `tomcat.version=10.1.60`: parche posterior al 10.1.55 del BOM.
- `postgresql.version=42.7.14`: parche posterior al 42.7.11 del BOM.
- `log4j2.version=2.26.1`: parche posterior al 2.24.3 del BOM.
- `jackson-bom.version=2.21.7`: corrige CVE-2026-54515, corregida desde 2.21.5.

No se forzaron versiones individuales de Spring Framework ni Spring Security. Maven Central oficial confirmó que 6.2.19 y 6.5.11 eran los últimos releases estables de sus ramas Java 17 al momento de esta ejecución; Spring Boot 4/Spring Framework 7 no se adoptaron porque requieren una migración mayor fuera de alcance.

## 3. Hallazgos del reporte inicial

El reporte inicial de Dependency-Check 13.0.0 analizó 112 dependencias y falló el build. Contenía **71 CVE únicos con CVSS >= 7**, incluidas asignaciones a Spring Boot 3.3.5, Spring Framework 6.1.14, Spring Security 6.3.4 y Tomcat 10.1.31.

La actualización eliminó del reporte activo, entre otros:

- Spring Boot: CVE-2026-22733, CVE-2026-40972, CVE-2026-40973, CVE-2026-40974, CVE-2026-40975, CVE-2026-40977 y CVE-2026-41001;
- Jackson: CVE-2026-54512, CVE-2026-54513, CVE-2026-54514 y CVE-2026-54515;
- PostgreSQL JDBC: CVE-2025-49146, CVE-2026-42198 y CVE-2026-54291;
- Log4j API: CVE-2026-34477, CVE-2026-34479 y CVE-2026-49844;
- Angus Mail: CVE-2025-7962;
- los hallazgos altos/críticos que el reporte anterior asignaba a Tomcat 10.1.31 y Spring Security 6.3.4;
- los hallazgos DOMPurify altos incluidos en Swagger UI 5.17.14.

La comparación se realizó contra los JSON de Dependency-Check y las versiones efectivas; no se asumió que cada asignación CPE fuera explotable.

## 4. Avisos Spring sin parche público Java 17

Después de actualizar a los últimos releases estables, NVD todavía asignó 15 CVE de CVSS >= 7 a artefactos Spring. Se revisaron las condiciones de los avisos oficiales y el código/árbol completo.

| Condición vulnerable | CVE | Evidencia de no aplicabilidad |
|---|---|---|
| XSLT o renderizado de vistas | CVE-2026-47884 | API REST JSON; no existen `XsltView`, vistas ni mapping de renderizado `/**`. |
| SSE, fragmentos o endpoints funcionales | CVE-2026-47890, CVE-2026-59313 | No existen SSE, `RouterFunction` ni framework funcional. |
| WebFlux/Aalto/PartEvent/Jetty/WebSocket | CVE-2026-47885, CVE-2026-47889, CVE-2026-47891, CVE-2026-47892, CVE-2026-47893 | Es Spring MVC/Tomcat; esas dependencias y APIs no están en el árbol ni en el código. |
| RSocket | CVE-2026-47888 | RSocket no está presente. |
| SpEL aportado por usuario | CVE-2026-47886, CVE-2026-59283 | No se evalúan expresiones SpEL de entrada. |
| Binding de property paths arbitrarios | CVE-2026-59282 | No hay `@ModelAttribute` ni binding de property paths; los cuerpos son DTO JSON. |
| LDAP UnboundID embebido | CVE-2026-59270 | No existe dependencia/configuración LDAP. |
| DPoP | CVE-2026-41707 | El recurso usa bearer JWT de Keycloak; no configura DPoP. |
| WebAuthn + sesión distribuida | CVE-2026-47841 | Seguridad stateless; no existen WebAuthn ni Spring Session. |

Se agregaron dos suppressions documentadas, limitadas por PURL a Spring Framework **6.2.19** y Spring Security **6.5.11**, y con vencimiento `2027-01-31Z`. Dependency-Check duplica CPE de framework entre varios JAR; por eso el patrón cubre los módulos de la misma familia y versión, pero no versiones futuras. No se suprimió ningún hallazgo sin revisar ni se redujo el umbral.

Fuentes primarias consultadas: avisos `https://spring.io/security/cve-<CVE>` y metadatos oficiales de Maven Central para Spring Boot, Framework y Security.

## 5. Dependency-Check final

Comando de control equivalente ejecutado:

```text
mvn --batch-mode org.owasp:dependency-check-maven:13.0.0:check
  -DautoUpdate=false
  -DdataDirectory=../dependency-check-data
  -Dformats=HTML,JSON,SARIF
  -DfailBuildOnCVSS=7.0
  -DsuppressionFile=../ops/dependency-check-suppressions.xml
```

Resultado: **BUILD SUCCESS**. Reportes generados en `backend/target/dependency-check-report.{html,json,sarif}`.

- activos con CVSS >= 7: **0**;
- suppressions revisadas de CVSS >= 7: **15 CVE únicos**;
- residuales activos: **14 avisos únicos** (11 `MEDIUM`, 1 `LOW` y 2 avisos RetireJS `low` sin CVSS NVD).

### Limitación de la clave NVD

`./ops/dependency-check.sh` se invocó, pero el proceso actual no heredó `NVD_API_KEY` y el wrapper se detuvo antes del análisis, sin imprimir ni almacenar ninguna clave. La base local `dependency-check-data/odc.mv.db` había sido actualizada el mismo día a las `2026-10-08 15:41:39 -03` por el análisis previo con NVD y fue usada sin red para el resultado final. Por lo tanto:

- el **análisis final sobre dependencias finales es válido con la base NVD local de ese día**;
- queda como verificación reproducible pendiente ejecutar nuevamente el wrapper exacto en un proceso que herede el secreto.

Sonatype OSS Index no se ejecutó porque actualmente exige credenciales propias; esta ausencia no alteró el analizador NVD ni el umbral del build.

## 6. Riesgos residuales por debajo del umbral

No hay crítica/alta activa. Los avisos medios/bajos se mantienen visibles, sin suppression:

- **CVE-2026-47834 (6.5), Spring Data JPA 3.5.13:** bypass de validación de `Sort`. El sistema recibe `Pageable`; varias rutas normalizan/permiten propiedades explícitas. No existe 3.5.14 estable en el BOM actual. Requiere seguimiento y revisión exhaustiva de todas las rutas de ordenamiento cuando se publique el parche.
- **CVE-2026-47842 (6.5) y CVE-2026-59276 (5.9), Spring Security:** no se encontró uso de `AesBytesEncryptor`; el segundo es un posible canal temporal en comparaciones internas. Seguir el próximo parche 6.5.x.
- **CVE-2026-47883, CVE-2026-47887, CVE-2026-59281, CVE-2026-59280 y CVE-2026-59314, Spring Framework:** requieren filtros/vistas/FreeMarker/renderizado o construcción vulnerable de cabeceras no presentes; permanecen visibles por estar bajo CVSS 7.
- **CVE-2023-33202 (Bouncy Castle 1.70):** transitiva de iText; afecta `PEMParser` con PEM hostil. El sistema genera PDF y no procesa PEM aportado por usuarios. La sustitución de los artefactos `jdk15on` requiere actualizar iText y se difiere para una fase compatible dedicada.
- **CVE-2024-47554 (Commons IO 2.11) y CVE-2025-48924 (Commons Lang 3.17):** transitivas; no se encontraron las APIs vulnerables `XmlStreamReader`/`ClassUtils.getClass` en el código.
- **CVE-2026-0871 (Keycloak Admin Client 26.0.12):** requiere un administrador `manage-users` y atributos no administrados. No hay versión 26.0.x posterior; el realm productivo nuevo no contiene usuarios demo ni atributos de ese tipo.
- **DOMPurify 3.4.13:** dos GHSA bajos dentro de Swagger UI 5.32.14. Swagger está deshabilitado en perfil productivo.

Estos riesgos no cumplen el umbral crítico/alto de aceptación, pero deben volver a evaluarse al aparecer parches compatibles.

## 7. Compatibilidad funcional y seguridad

`mvn clean test --batch-mode` finalizó el 2026-10-08 a las 16:11:47 -03:

```text
Tests run: 325, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

La suite cubrió servicios y controladores de autenticación/roles, audience JWT, fotografías, auditoría, usuarios Keycloak, exhibiciones, devoluciones, inventario, almacenamiento, validación, errores e integración PostgreSQL/concurrencia. No se eliminaron ni deshabilitaron tests.

## 8. Base de datos y Flyway

- V1–V30 sin cambios en Git.
- Base vacía: validada por Testcontainers y arranque productivo.
- Upgrade desde V28: `FlywayV28UpgradeIntegrationTest.actualizaV28ALatestSinDemoYConservaCatalogos` pasó dentro de la suite.
- PostgreSQL: 16.11.
- Stack productivo: última fila `flyway_schema_history` = `30|true`.
- Conexión runtime observada: `museo_app`.
- `museo_app` y `museo_migrator`: `NOSUPERUSER`, `NOCREATEDB`, `NOCREATEROLE`, `NOBYPASSRLS`.

El usuario bootstrap conserva atributos elevados sólo para provisionar roles/base al inicio; no es el usuario runtime de la aplicación.

## 9. Keycloak y JWT

- imagen/running version: **Keycloak 26.8.0**;
- PostgreSQL Keycloak: healthy y persistente;
- roles del realm: `ADMIN`, `MUSEOLOGO`, `VIEWER`;
- usuarios habilitados en el realm limpio: 0; usuarios cuyo nombre contiene `demo`: 0;
- emisión de token `client_credentials`: exitosa;
- un token administrativo sin `aud=museo-backend` recibió **HTTP 401** del backend, confirmando que el audience continúa siendo obligatorio;
- las pruebas de seguridad verificaron autorización ADMIN/MUSEOLOGO/VIEWER y JWT válido.

No se ejecutó login de un usuario final real porque el realm productivo aislado se creó deliberadamente sin usuarios demo.

## 10. Docker productivo

Se ejecutó:

```text
docker compose --env-file .phase5-runtime/test.env \
  -f docker-compose.prod.yml up --build --wait
```

Después del override Jackson se reconstruyó y recreó nuevamente el backend desde el POM final. Imagen final backend: `sha256:6b14a619a674ca2470420a4e47a189ef6c3edb2ae1b801d2418e9a890eef7243`.

Estado final:

- `postgres-museo`: healthy;
- `postgres-keycloak`: healthy;
- `keycloak`: healthy;
- `backend`: healthy, actuator `UP`;
- `frontend`: healthy;
- `reverse-proxy`: healthy.

El entorno fue sintético y aislado (`museo-phase5-dbtest`); no se usaron datos reales.

## 11. Archivos modificados

- `backend/pom.xml`: versiones/BOM.
- `ops/dependency-check-suppressions.xml`: dos grupos exactos, documentados y con vencimiento.
- `PRE_PRODUCTION_FIX_PHASE_6.md`: este informe.

`PRE_PRODUCTION_RELEASE_VERIFICATION.md` ya estaba sin seguimiento antes de iniciar esta fase y no fue modificado. No se realizó commit ni push.

## 12. Criterios de aceptación

| Criterio | Estado |
|---|---|
| Java 17 | **CUMPLIDO** |
| Backend compila | **CUMPLIDO** |
| 325 tests pasan | **CUMPLIDO** |
| Flyway vacío/V28/V30 | **CUMPLIDO** |
| Keycloak 26.8.0 y roles/JWT | **CUMPLIDO** |
| Docker productivo | **CUMPLIDO** |
| Sin crítica/alta aplicable | **CUMPLIDO** |
| Sin regresión observada | **CUMPLIDO** |
| Wrapper online con `NVD_API_KEY` heredada | **PENDIENTE DE REPRODUCCIÓN**; análisis final ejecutado con base NVD local actualizada ese día |

## 13. Conclusión

La remediación funcional de dependencias backend queda **completada para vulnerabilidades críticas y altas aplicables**. El único punto no reproducido exactamente fue la actualización online del wrapper desde este proceso por ausencia de `NVD_API_KEY`; no invalida el reporte final contra la base NVD actualizada ese día, pero debe repetirse en CI o en una shell que herede el secreto antes de fijar el artefacto de release.
