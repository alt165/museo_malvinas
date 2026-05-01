# 🤖 agents.md — Backend Museo (Codex AI Agent Guide)

## 🎯 Propósito

Este documento define las reglas, contexto y restricciones para el agente de inteligencia artificial que genera código para el backend del sistema de inventario del museo.

El objetivo es garantizar:

* Consistencia en la arquitectura
* Código mantenible
* Buenas prácticas
* Evitar decisiones arbitrarias del agente

---

## 🧩 Contexto del Proyecto

El sistema es un backend para la gestión de un museo que incluye:

* Inventario de objetos históricos (armas, fotos, cartas, etc.)
* Relaciones entre objetos
* Gestión de usuarios con distintos niveles de autorización
* Auditoría de cambios
* API REST para consumo externo
* Seguridad basada en autenticación y autorización

---

## 🏗️ Stack Tecnológico

El agente DEBE usar:

* Java 17+
* Spring Boot
* Spring Web
* Spring Data JPA
* PostgreSQL
* Docker / Docker Compose
* Keycloak (para autenticación y autorización)
* Lombok (para reducir boilerplate)

---

## 🧱 Arquitectura

El backend sigue una arquitectura en capas estricta:

```
controller → service → repository → database
```

Separaciones obligatorias:

* Controllers: solo manejan requests/responses
* Services: lógica de negocio
* Repositories: acceso a datos
* Entities: representación de base de datos, con borrado lógico mediante `activo`, `eliminado` y `fechaEliminacion`
* DTOs: comunicación externa

---

## 📦 Estructura de paquetes

El agente DEBE usar esta estructura:

```
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

## 📌 Reglas de Desarrollo

### 1. Controllers

* No contienen lógica de negocio
* Solo llaman a servicios
* Usan DTOs (NO entidades)

---

### 2. Services

* Contienen la lógica de negocio
* Validan datos
* Manejan reglas del sistema
* No acceden directamente a HTTP

---

### 3. Repositories

* Usan Spring Data JPA
* No contienen lógica de negocio

---

### 4. Entities

* Anotadas con JPA
* Usan Lombok
* Representan tablas reales
* Usan borrado lógico con `activo`, `eliminado` y `fechaEliminacion`
* No persisten roles locales
* Usan `Exhibicion` como único nombre para muestras del museo
* `ObjetoDigital` debe heredar de `ObjetoMuseo` con estrategia `JOINED` y no representar un archivo asociado

---

### 5. DTOs

* Se usan SIEMPRE para entrada/salida
* No exponer entidades directamente

---

### 6. Mappers

* Separar conversión Entity ↔ DTO
* Preferentemente manual o con MapStruct

---

## 🔐 Seguridad

El sistema utiliza Keycloak.

El agente DEBE:

* Configurar Spring Security
* Usar JWT
* No implementar autenticación propia
* Proteger endpoints según roles
* No persistir roles localmente; Keycloak es la fuente de verdad

Roles esperados en JWT:

* SUDO
* ADMIN
* OPERATOR
* VIEWER

---

## 🐳 Docker

El agente DEBE generar:

* Dockerfile para backend
* docker-compose.yml con:

  * backend
  * base de datos PostgreSQL

---

## 🧠 Convenciones de Código

### Naming

* Clases: PascalCase
* Variables: camelCase
* Métodos: camelCase
* Constantes: UPPER_CASE

---

### Buenas prácticas obligatorias

* Inyección por constructor
* Uso de `@Service`, `@RestController`, etc.
* Manejo de excepciones global (`@ControllerAdvice`)
* Validaciones con `@Valid`

---

## 🧾 Base de Datos

* PostgreSQL
* Uso de índices cuando sea necesario
* Soporte para búsqueda de texto completo (tsvector)
* Relaciones bien definidas (FKs)
* Entidades intermedias obligatorias para relaciones N:N con control de dominio: `ObjetoCategoria`, `ObjetoDepositante`, `ObjetoVeterano`, `ExhibicionObjeto` y `RelacionObjeto`

---

## 🔄 Flujo de Desarrollo (OBLIGATORIO)

El agente DEBE seguir este orden SIEMPRE:

1. Entidades
2. Repositorios
3. DTOs
4. Servicios
5. Controladores
6. Seguridad
7. Tests

NO saltar pasos.

---

## 🚫 Restricciones

El agente NO debe:

* Mezclar lógica en controllers
* Exponer entidades directamente
* Crear endpoints sin DTOs
* Ignorar la arquitectura definida
* Inventar tecnologías no especificadas
* Implementar autenticación sin Keycloak
* Escribir código sin explicación cuando se solicite

---

## 🧪 Testing

El agente debe generar:

* Tests unitarios para servicios
* Tests de integración para controllers

---

## 📡 API

* Estilo REST
* JSON como formato de intercambio
* Uso correcto de HTTP status codes

---

## 🧭 Criterios de Calidad

El código generado debe ser:

* Legible
* Modular
* Escalable
* Fácil de mantener

---

## 🧑‍💻 Forma de trabajo del agente

El agente debe:

* Trabajar de forma incremental
* No generar todo el sistema en una sola respuesta
* Esperar validación antes de continuar
* Explicar decisiones si se le solicita

---

## 📌 Notas adicionales

* El sistema está orientado a escalabilidad futura
* Se prioriza claridad sobre “magia”
* Se busca evitar deuda técnica

---

## ✅ Resultado esperado

Un backend robusto, seguro y mantenible, alineado con buenas prácticas profesionales y listo para ser desplegado en contenedores Docker.

