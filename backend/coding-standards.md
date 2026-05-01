# 📐 coding-standards.md — Backend Museo

## 🎯 Objetivo

Definir estándares de código obligatorios para el desarrollo del backend del sistema de inventario del museo.

Este documento asegura:

* Consistencia
* Legibilidad
* Mantenibilidad
* Buenas prácticas

---

## 🧩 Principios Generales

* Código claro > código “inteligente”
* Separación estricta de responsabilidades
* Evitar duplicación
* Preferir simplicidad
* No introducir lógica innecesaria

---

## 📛 Convenciones de Nombres

### Clases

* PascalCase
  Ej: `ObjetoMuseo`, `VeteranoService`

### Variables

* camelCase
  Ej: `fechaInicio`, `estadoInventario`

### Métodos

* camelCase
  Ej: `obtenerObjetos()`, `crearExposicion()`

### Constantes

* UPPER_CASE
  Ej: `MAX_USUARIOS`

---

## 📦 Estructura de Clases

Cada clase debe tener:

1. Atributos
2. Constructor
3. Métodos públicos
4. Métodos privados

Orden consistente en todo el proyecto.

---

## 🧱 Entidades JPA

### Reglas obligatorias

* Usar `@Entity`
* Usar `@Table(name = "...")`
* Usar `@Id` + `@GeneratedValue`
* Usar Lombok (`@Getter`, `@Setter`)

---

### Relaciones

#### ❗ Regla crítica

NO usar relaciones N:N directas con `@ManyToMany` si hay atributos adicionales.

👉 En su lugar:

* Crear entidad intermedia

Ejemplo:

* `ObjetoVeterano`
* `ExposicionObjeto`

---

### Buenas prácticas

* Usar `FetchType.LAZY` por defecto
* Evitar `EAGER` salvo casos justificados
* Definir `mappedBy` correctamente
* Evitar relaciones bidireccionales innecesarias

---

## 🔁 DTOs

### Reglas

* Nunca exponer entidades directamente
* Usar DTOs para:

  * Request
  * Response

---

### Naming

* `ObjetoMuseoRequestDTO`
* `ObjetoMuseoResponseDTO`

---

### Contenido

* Solo datos necesarios
* No incluir lógica

---

## 🔄 Mappers

* Separar conversión Entity ↔ DTO
* No mapear dentro de controllers
* No mapear dentro de repositories

---

## 🎮 Controllers

### Reglas

* Usar `@RestController`
* No contener lógica de negocio
* Solo:

  * recibir request
  * validar
  * delegar al service
  * devolver response

---

### Ejemplo correcto

```id="ctrlok1"
@PostMapping
public ResponseEntity<ObjetoMuseoResponseDTO> crear(@RequestBody @Valid ObjetoMuseoRequestDTO dto) {
    return ResponseEntity.ok(service.crear(dto));
}
```

---

## ⚙️ Services

### Reglas

* Contienen lógica de negocio
* Usar `@Service`
* Inyección por constructor

---

### Prohibido

* Acceder directamente a HTTP
* Usar `@RequestBody` o `@PathVariable`

---

## 🗄️ Repositories

* Extender `JpaRepository`
* No lógica de negocio
* Solo queries

---

## ❗ Manejo de Excepciones

### Reglas

* Usar `@ControllerAdvice`
* Crear excepciones personalizadas

Ejemplo:

* `ResourceNotFoundException`
* `BusinessException`

---

## 🔐 Seguridad

* No implementar autenticación manual
* Usar Keycloak
* Validar roles en controllers o configuración

---

## 🐳 Docker

* No hardcodear configuraciones
* Usar variables de entorno
* Mantener Dockerfile simple

---

## 🧪 Testing

### Unit tests

* Services

### Integration tests

* Controllers

---

## 🧠 Lógica de Negocio

### Reglas

* Debe estar en Services
* Debe ser reutilizable
* Debe ser testeable

---

## ⚠️ Anti-patterns prohibidos

❌ Lógica en controllers
❌ Exponer entidades
❌ Usar `@ManyToMany` sin control
❌ Código duplicado
❌ Métodos demasiado largos (>50 líneas)
❌ Clases “Dios”

---

## 📏 Tamaño del Código

* Métodos: máximo ~50 líneas
* Clases: responsabilidad única

---

## 🔍 Logging

* Usar logs en:

  * errores
  * eventos importantes

* No loggear información sensible

---

## 📡 API

* Respuestas en JSON
* Usar códigos HTTP correctos
* Mensajes de error claros

---

## 🧭 Flujo de Desarrollo

El agente DEBE seguir:

1. Entidades
2. DTOs
3. Repositories
4. Services
5. Controllers

---

## ✅ Checklist antes de generar código

El agente debe verificar:

* ¿Se respeta la arquitectura?
* ¿Se usan DTOs?
* ¿La lógica está en services?
* ¿Las relaciones JPA están bien modeladas?

---

## 🎯 Resultado esperado

Código:

* Limpio
* Consistente
* Escalable
* Fácil de mantener

