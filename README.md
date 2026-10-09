# Trabajo final Ingeniería de Software I - 2026 - Ingeniería en Computación UNRN

## *Sistema de Gestión de Archivo Museo Malvinas, Antártida y Atlántico Sur* ##

- *Alumnos:* Bravo Gonzalo, Lambrechts Jose, Tummino Lautaro, Velazquez Laura

### `Descripción del proyecto`

 El Sistema de Gestión de Archivo es una aplicación diseñada para administrar, registrar y localizar objetos, documentación y material audiovisual del Museo Malvinas, Antártida y Atlántico Sur ubicado en San Carlos de Bariloche. El sistema permite gestionar de manera integral el inventario patrimonial de la institución, incluyendo el registro de objetos, colecciones, depositantes, ingresos, ubicaciones físicas, exhibiciones y la información histórica de los veteranos de guerra vinculados al museo. Su arquitectura se compone de un Frontend desarrollado con tecnologías web modernas, un Backend Spring Boot sobre Java 17, base de datos PostgreSQL y gestión de acceso mediante Keycloak.

Para producción, usar exclusivamente [`docker-compose.prod.yml`](docker-compose.prod.yml) y seguir [`PRODUCTION_DEPLOYMENT.md`](PRODUCTION_DEPLOYMENT.md). El `docker-compose.yml` principal es sólo para desarrollo local.

La plataforma productiva versionada utiliza Java 17, PostgreSQL 16.11, Keycloak 26.8.0 y migraciones Flyway hasta V30. Los datos demostrativos no forman parte de una instalación productiva nueva; los fixtures de integración viven únicamente bajo `backend/src/test/resources`.

### `Instalación`

- Instalar [Docker](https://www.docker.com/get-started) y Docker Compose
- Clonar el repositorio y ubicarse en la carpeta raíz desde una terminal
- Ejecutar el comando `docker-compose up -d --build`
- La base de datos (PostgreSQL) se inicializa y se carga con los datos base automáticamente con Flyway
- Se ejecuta en un navegador web ingresando a [http://localhost:3000](http://localhost:3000)
- Para detener el sistema, ejecutar `docker-compose down`

*Este software fue desarrollado en el contexto de la cátedra Ingeniería de Software I (UNRN, Argentina)*
