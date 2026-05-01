# 🏗️ architecture.md — Backend Sistema de Inventario de Museo

## 🎯 Objetivo

Definir la arquitectura del backend del sistema de inventario del museo, incluyendo:

* Organización del sistema
* Decisiones tecnológicas
* Flujo de datos
* Seguridad
* Escalabilidad

Este documento guía tanto a desarrolladores como a agentes de IA para mantener consistencia estructural.

---

## 🧩 Descripción General del Sistema

El sistema es un backend que permite:

* Gestionar objetos del museo (armas, fotos, documentos, etc.)
* Administrar inventario
* Relacionar objetos entre sí
* Gestionar usuarios y permisos
* Auditar cambios
* Exponer una API REST

---

## 🏛️ Estilo Arquitectónico

Se utiliza una arquitectura en capas (Layered Architecture):

```id="k2y9lq"
Controller → Service → Repository → Database
```

### 📌 Responsabilidades por capa

#### Controller

* Maneja requests HTTP
* Valida entrada (DTOs)
* Devuelve respuestas HTTP

#### Service

* Contiene la lógica de negocio
* Orquesta operaciones
* Aplica reglas del dominio

#### Repository

* Acceso a datos
* Interacción con la base de datos mediante JPA

#### Database

* Persistencia en PostgreSQL

---

## 📦 Estructura del Proyecto

```id="0o0qv3"
com.proveedores
│
├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── config
├── security
├── exception
```

---

## 🧠 Modelo de Dominio (Resumen)

### Entidades principales:

* Usuario
* ObjetoMuseo
* ObjetoDigital
* CategoriaObjeto
* ObjetoCategoria
* Inventario
* MovimientoInventario
* Auditoria
* Depositante
* ObjetoDepositante
* Veterano
* Ubicacion
* Exhibicion

### Características:

* Los objetos tienen categorías y un objeto puede tener más de una categoría mediante la entidad intermedia ObjetoCategoria
* ObjetoDigital es un subtipo de ObjetoMuseo usando herencia JPA JOINED; no representa un archivo asociado ni guarda una URL de archivo
* El inventario controla ubicación, fecha de ingreso, de salida y estado de conservacion
* Auditoría registra cambios históricos
* Una exhibición puede ser permanente o temporal y al finalizar debe permitir registrar los objetos devueltos mediante ExhibicionObjeto
* Un veterano tiene un detalle de actuacion en la guerra
* Solo se permite borrado lógico: las entidades persistentes deben incluir activo, eliminado y fechaEliminacion

---


## 📌 Decisiones de Modelo Vigentes

* Se usa el nombre `Exhibicion` en todo el dominio.
* `ObjetoDigital` hereda de `ObjetoMuseo` con estrategia JPA `JOINED` y no representa un archivo asociado.
* `Usuario` es solo referencia local a Keycloak; no hay entidad ni tabla local de roles.
* El borrado físico está prohibido; usar `activo`, `eliminado` y `fechaEliminacion`.
* Un `ObjetoMuseo` puede tener múltiples `CategoriaObjeto` mediante `ObjetoCategoria`.
* Un `ObjetoMuseo` puede tener múltiples `Depositante` mediante `ObjetoDepositante`.

---

## 🔄 Flujo de Datos

### Flujo típico de una request:

```id="pkj03z"
Cliente → Controller → Service → Repository → Database
                  ↓
               DTO ↔ Entity
```

1. Cliente envía request HTTP
2. Controller recibe DTO
3. Service procesa lógica
4. Repository accede a DB
5. Se transforma Entity → DTO
6. Se devuelve response

---

## 🔐 Seguridad

### Tecnología

* Keycloak
* JWT (Bearer Token)

### Flujo de autenticación

```id="knk4gk"
Cliente → Keycloak → Token JWT → Backend → Validación → Acceso permitido/denegado
```

### Autorización

Roles definidos:

* SUDO → superusuario, responsable de todo el sistema
* ADMIN → acceso total a las tablas administrativas sin poder dar de alta usuarios
* OPERATOR → gestión de inventario
* VIEWER → solo lectura

### Consideraciones

* El backend NO gestiona usuarios directamente (lo hace Keycloak)
* Los roles no se persisten localmente; se leen desde el JWT emitido por Keycloak
* Se validan roles en endpoints protegidos
* Se usa Spring Security

---

## 🐳 Contenerización

El sistema se ejecuta en contenedores Docker.

### Servicios definidos:

* backend (Spring Boot)
* database (PostgreSQL)

### Archivo principal:

* docker-compose.yml

---

## 🗄️ Base de Datos

### Motor

* PostgreSQL

### Características

* Uso de claves foráneas
* Índices para optimización
* Soporte para búsqueda de texto completo (tsvector)

### Ejemplos de optimización

* Índices por campos de búsqueda frecuente
* Índices GIN para texto

---

## 🔍 Búsqueda

Se implementa búsqueda avanzada:

* Full-text search con PostgreSQL
* Posibilidad de filtrar por:

  * categoría
  * tipo de objeto
  * estado
  * fecha de ingreso
  * depositante

---


## 🏷️ Relación: ObjetoMuseo ↔ CategoriaObjeto

Tipo: N:N

Implementación:

* Tabla intermedia: `objeto_categoria`
* Entidad intermedia: `ObjetoCategoria`

Motivo:

* Un objeto puede tener más de una categoría.
* Una categoría puede clasificar múltiples objetos.
* Se evita `@ManyToMany` directo para mantener control del modelo y permitir atributos futuros.

---

## 🤝 Relación: ObjetoMuseo ↔ Depositante

Tipo: N:N

Implementación:

* Tabla intermedia: `objeto_depositante`
* Entidad intermedia: `ObjetoDepositante`

Motivo:

* Un objeto puede tener más de un depositante.
* Un depositante puede estar asociado a múltiples objetos.
* La relación puede requerir fecha, tipo de depósito, observaciones o documentación en el futuro.

---

## 🔗 Relaciones entre Objetos

Se permite:

* Relación N:N entre objetos
* Tipos de relación (ej: "pertenece a", "usado en", etc.)

Implementación:

* Tabla intermedia `relacion_objeto`

---

## 🧾 Auditoría

Se registra:

* Creación
* Modificación
* Eliminación

Datos almacenados:

* Usuario
* Fecha
* Tipo de operación
* Datos previos (opcional)

---

## ⚙️ Configuración

Se centraliza en:

* `application.yml`
* Variables de entorno (Docker)

---

## 🧪 Testing

### Tipos

* Unitarios (services)
* Integración (controllers)

---

## 📡 API REST

### Estilo

* RESTful
* JSON

### Convenciones

* GET → obtener datos
* POST → crear
* PUT → actualizar
* DELETE → eliminar

### Códigos HTTP

* 200 OK
* 201 CREATED
* 400 BAD REQUEST
* 401 UNAUTHORIZED
* 403 FORBIDDEN
* 404 NOT FOUND

---

## 📈 Escalabilidad

El sistema está diseñado para:

* Escalar horizontalmente (Docker)
* Integrarse con frontend o apps externas

---

## 🚧 Decisiones Arquitectónicas

### ¿Por qué Spring Boot?

* Ecosistema robusto
* Integración con seguridad y JPA

---

### ¿Por qué PostgreSQL?

* Soporte para relaciones complejas
* Full-text search
* Estabilidad

---

### ¿Por qué Keycloak?

* Gestión centralizada de identidad
* Soporte OAuth2/OpenID
* Evita implementar seguridad manual

---

### ¿Por qué Docker?

* Portabilidad
* Entornos reproducibles
* Fácil despliegue

---

## ⚠️ Consideraciones Importantes

* NO exponer entidades directamente
* Usar DTOs siempre
* Mantener separación de capas estricta
* Validar datos en entrada
* Manejar errores de forma centralizada

---

## 🧭 Evolución futura

Posibles mejoras:

* Cache (Redis)
* Sistema de eventos
* Panel de administración avanzado

---

## ✅ Resultado esperado

Un backend:

* Modular
* Seguro
* Escalable
* Fácil de mantener
* Preparado para crecimiento futuro

## 🧠 Extensión del Modelo de Dominio

Se incorporan nuevas entidades clave para representar:

* Veteranos de guerra
* Exhibiciones del museo
* Relación entre objetos y veteranos
* Control de préstamos/devoluciones en exhibiciones

---

## 👤 Entidad: Veterano

Representa personas vinculadas históricamente a los objetos del museo.

### Atributos principales:

* id
* nombre
* apellido
* rango (opcional)
* fuerza (ej: Ejército, Armada, Fuerza Aérea)
* fechaNacimiento (opcional)
* fechaFallecimiento (opcional)
* descripcion / historia

### Relaciones:

* Relación N:N con ObjetoMuseo

Un veterano puede estar asociado a múltiples objetos, y un objeto puede estar asociado a múltiples veteranos.

---

## 🔗 Relación: ObjetoMuseo ↔ Veterano

Tipo: N:N

Implementación:

* Tabla intermedia: `objeto_veterano`

### Posibles atributos adicionales:

* tipoRelacion (ej: "propietario", "usuario", "relacionado históricamente")
* descripcion

---

## 🖼️ Entidad: Exhibicion

Representa exhibiciones organizadas por el museo.

### Atributos principales:

* id
* nombre
* descripcion
* tipo (ENUM: TEMPORAL, PERMANENTE)
* fechaInicio
* fechaFin (nullable para permanentes)
* estado (ENUM: PLANIFICADA, ACTIVA, FINALIZADA)

---

## 🔗 Relación: Exhibicion ↔ ObjetoMuseo

Tipo: N:N

Implementación:

* Tabla intermedia: `exhibicion_objeto`

### Atributos adicionales:

* fechaInclusion
* fechaRetiro (cuando el objeto vuelve al inventario)
* estado (ENUM: EN_EXHIBICION, DEVUELTO, PENDIENTE_REVISION)

---

## 📦 Lógica de Inventario en Exhibiciones

Cuando un objeto entra en una exhibición:

* Se marca como “fuera de inventario disponible”
* Se registra en `exhibicion_objeto`

Cuando la exhibición finaliza:

* Los objetos deben ser devueltos al inventario
* Se requiere validación manual

---

## ✅ Proceso de Verificación de Devolución

Se define un flujo obligatorio:

1. La exhibición pasa a estado FINALIZADA
2. Se listan todos los objetos asociados
3. Un usuario autorizado verifica cada objeto
4. Se marca como DEVUELTO
5. El objeto vuelve a estar disponible en inventario

---

## 🔐 Reglas de Negocio

* Solo usuarios con rol ADMIN u OPERATOR pueden:

  * Crear exhibiciones
  * Asociar objetos
  * Finalizar exhibiciones
  * Confirmar devoluciones

* No se puede:

  * Eliminar una exhibición activa
  * Marcar como devuelto un objeto que no pertenece a la exhibición
  * Finalizar una exhibición con objetos no verificados

---

## ⚙️ Impacto en el Sistema

### Cambios en entidades existentes:

#### ObjetoMuseo

Debe incluir:

* relación con Veterano mediante ObjetoVeterano
* relación con Exhibicion mediante ExhibicionObjeto
* relación con CategoriaObjeto mediante ObjetoCategoria
* relación con Depositante mediante ObjetoDepositante

El estado de disponibilidad no se duplica en ObjetoMuseo; debe resolverse desde Inventario y MovimientoInventario.

---

### Inventario

Debe considerar:

* objetos temporalmente fuera por exhibición
* trazabilidad de movimientos

---

## 🔄 Flujo de Exhibición

```id="exhflow1"
Creación → Asociación de objetos → Activación → Uso en exhibición → Finalización → Verificación → Reingreso a inventario
```

---

## 🧪 Consideraciones para Testing

Se deben cubrir casos:

* Objeto en múltiples exhibiciones (histórico)
* Exhibición sin fecha de fin (permanente)
* Fallo en devolución de objetos
* Validación de permisos

---

## 📈 Escenarios futuros

* Exhibiciones itinerantes
* Préstamo a otros museos
* Seguimiento logístico de objetos


