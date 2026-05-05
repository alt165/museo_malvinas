# API

## Base URL

Local con Docker:

```text
http://localhost:8080
```

Si se levanta con `BACKEND_PORT=18080`:

```text
http://localhost:18080
```

Todas las rutas funcionales usan el prefijo `/api`.

## Autenticacion

Salvo Actuator health/info y Swagger local, los endpoints `/api/**` requieren JWT Bearer:

```http
Authorization: Bearer <access_token>
```

Permisos generales actuales:

- `GET /api/**`: `ADMIN`, `OPERATOR`, `VIEWER`
- `POST /api/**`: `ADMIN`, `OPERATOR`
- `PUT /api/**`: `ADMIN`, `OPERATOR`
- `PATCH /api/**`: `ADMIN`, `OPERATOR`
- `DELETE /api/**`: `ADMIN`, `OPERATOR`

## Convenciones CRUD

La mayoria de controllers implementan:

```text
POST   /api/<recurso>       -> crear, responde 201
GET    /api/<recurso>/{id}  -> obtener por id, responde 200
GET    /api/<recurso>       -> listar, responde 200
PUT    /api/<recurso>/{id}  -> actualizar, responde 200
DELETE /api/<recurso>/{id}  -> baja logica, responde 204
```

## Endpoints principales

| Recurso | Base path | Notas |
| --- | --- | --- |
| Objetos de museo | `/api/objetos` | Alta y mantenimiento de objetos patrimoniales. |
| Objetos digitales | `/api/objetos-digitales` | Subtipo de objeto museo con metadatos digitales. |
| Inventarios | `/api/inventarios` | Ubicacion, estado y conservacion de objetos. |
| Movimientos de inventario | `/api/movimientos-inventario` | Registro de movimientos sobre objetos. |
| Exhibiciones | `/api/exhibiciones` | Muestras temporales o permanentes. |
| Objetos en exhibiciones | `/api/exhibiciones-objetos` | Asignacion y devolucion de objetos. |
| Veteranos | `/api/veteranos` | Datos de veteranos. |
| Actuaciones de veteranos | `/api/actuaciones-veteranos` | Participacion, unidad, rol y periodo. |
| Depositantes | `/api/depositantes` | Personas o instituciones depositantes. |
| Categorias | `/api/categorias` | Clasificacion de objetos. |
| Ubicaciones | `/api/ubicaciones` | Lugares fisicos/logicos del museo. |
| Relaciones entre objetos | `/api/relaciones-objetos` | Vinculos semanticos entre objetos. |

## Endpoints especiales

### Finalizar exhibicion

```http
POST /api/exhibiciones/{id}/finalizar
```

Regla: no permite finalizar si existen objetos asociados con devolucion pendiente.

### Verificar devolucion de objeto

```http
POST /api/exhibiciones-objetos/{id}/verificar-devolucion?usuarioId=1&observaciones=Devuelto%20sin%20observaciones
```

Marca la devolucion como verificada y actualiza el estado del objeto de exhibicion.

## Ejemplos

### Crear objeto de museo

```bash
curl -X POST http://localhost:8080/api/objetos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "numeroInventario": "MM-2026-001",
    "nombre": "Casco de combate",
    "tipoObjeto": "Equipo",
    "descripcion": "Objeto historico catalogado para inventario."
  }'
```

Respuesta:

```json
{
  "id": 1,
  "numeroInventario": "MM-2026-001",
  "nombre": "Casco de combate",
  "tipoObjeto": "Equipo",
  "descripcion": "Objeto historico catalogado para inventario."
}
```

### Crear inventario

```bash
curl -X POST http://localhost:8080/api/inventarios \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "objetoMuseoId": 1,
    "ubicacionId": 1,
    "estado": "DISPONIBLE",
    "estadoConservacion": "BUENO",
    "fechaIngreso": "2026-05-05",
    "fechaSalida": null,
    "observaciones": "Ingreso inicial."
  }'
```

### Crear exhibicion

```bash
curl -X POST http://localhost:8080/api/exhibiciones \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Malvinas: memoria y soberania",
    "descripcion": "Exhibicion temporal.",
    "tipo": "TEMPORAL",
    "fechaInicio": "2026-05-05",
    "fechaFin": null,
    "estado": "ACTIVA"
  }'
```

### Asociar objeto a exhibicion

```bash
curl -X POST http://localhost:8080/api/exhibiciones-objetos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "exhibicionId": 1,
    "objetoMuseoId": 1,
    "fechaInclusion": "2026-05-05",
    "fechaRetiro": null,
    "estado": "EN_EXHIBICION",
    "devolucionVerificada": false,
    "verificadoPorUsuarioId": null,
    "fechaVerificacion": null,
    "observacionesDevolucion": null
  }'
```

### Obtener health readiness

```bash
curl http://localhost:8080/actuator/health/readiness
```

Respuesta:

```json
{"status":"UP"}
```

## Respuestas de error

Formato:

```json
{
  "timestamp": "2026-05-05T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene errores de validacion",
  "path": "/api/objetos",
  "validationErrors": {
    "numeroInventario": "El numero de inventario es obligatorio"
  }
}
```

Codigos habituales:

- `400`: validacion o regla de negocio.
- `401`: autenticacion requerida.
- `403`: permisos insuficientes.
- `404`: recurso inexistente o eliminado logicamente.
- `409`: violacion de restriccion de datos.
- `500`: error interno no controlado.

