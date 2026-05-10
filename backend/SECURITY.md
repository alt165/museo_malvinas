# Security

## Modelo de seguridad

El backend usa Spring Security como OAuth2 Resource Server y Keycloak como proveedor de identidad.

La aplicacion no implementa login propio. El flujo esperado es:

```text
Cliente -> Keycloak -> access token JWT -> Backend -> validacion JWT -> autorizacion por rol
```

## Configuracion local de Keycloak

El entorno Docker importa el realm desde:

```text
docker/keycloak/museo-realm.json
```

Realm:

```text
museo
```

Clientes locales:

- `museo-backend`: cliente bearer-only usado por el backend.
- `museo-local`: cliente publico con direct access grants habilitado para desarrollo local.
- `museo-admin`: cliente confidencial de service account usado por el backend para Keycloak Admin API.

Usuarios locales de desarrollo:

| Usuario | Password | Rol |
| --- | --- | --- |
| `admin` | `admin` | `ADMIN` |
| `operator` | `operator` | `OPERATOR` |
| `viewer` | `viewer` | `VIEWER` |

Estas credenciales son solo para desarrollo local y no deben usarse en produccion.

## Obtener token en local

Con Keycloak levantado en `http://localhost:8081`:

```bash
curl -X POST http://localhost:8081/realms/museo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=museo-local" \
  -d "username=admin" \
  -d "password=admin"
```

La respuesta contiene `access_token`. Para usarlo:

```bash
export TOKEN="<access_token>"
curl http://localhost:8080/api/objetos \
  -H "Authorization: Bearer $TOKEN"
```

## Roles y permisos

Roles configurados actualmente en el realm local:

- `ADMIN`
- `OPERATOR`
- `VIEWER`

Permisos aplicados por `SecurityConfig`:

| Metodo | Path | Roles permitidos |
| --- | --- | --- |
| Cualquiera | `/api/admin/**` | `ADMIN` |
| `GET` | `/api/**` | `ADMIN`, `OPERATOR`, `VIEWER` |
| `POST` | `/api/**` | `ADMIN`, `OPERATOR` |
| `PUT` | `/api/**` | `ADMIN`, `OPERATOR` |
| `PATCH` | `/api/**` | `ADMIN`, `OPERATOR` |
| `DELETE` | `/api/**` | `ADMIN`, `OPERATOR` |

La documentacion base del proyecto menciona `SUDO`, pero el realm local y la configuracion actual del backend no lo habilitan en reglas de acceso.

## Administracion de usuarios y DNI

Los usuarios reales viven en Keycloak. El backend no implementa login propio, no guarda contrasenas y no persiste DNI en PostgreSQL.

El modulo `/api/admin/usuarios` requiere `ADMIN` y usa Keycloak Admin API. El DNI es obligatorio para altas y actualizaciones, se trata como `String` y se almacena en Keycloak como atributo custom:

```json
{
  "attributes": {
    "dni": ["12345678"]
  }
}
```

El DNI no se usa para autenticar y no reemplaza al username.

## Extraccion de roles

`KeycloakJwtAuthenticationConverter` lee roles desde:

- `realm_access.roles`
- `resource_access[KEYCLOAK_CLIENT_ID].roles`

Cada rol se transforma en authority Spring Security con prefijo `ROLE_`.

Ejemplo:

```json
{
  "realm_access": {
    "roles": ["ADMIN"]
  }
}
```

Se convierte en:

```text
ROLE_ADMIN
```

## Endpoints publicos

Publicos sin JWT:

- `/actuator/health`
- `/actuator/health/**`
- `/actuator/info`
- Swagger local:
  - `/swagger-ui.html`
  - `/swagger-ui/**`
  - `/v3/api-docs`
  - `/v3/api-docs/**`

En perfil `prod`, Swagger se deshabilita por configuracion:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## CORS

Los origins permitidos se configuran con:

```text
APP_CORS_ALLOWED_ORIGINS
```

Default local:

```text
http://localhost:3000,http://localhost:5173
```

Headers permitidos:

- `Authorization`
- `Content-Type`
- `X-Request-Id`

Header expuesto:

- `X-Request-Id`

## Logs y datos sensibles

Los logs incluyen `requestId`, usuario, metodo, endpoint y status. No se loguean tokens JWT, passwords, DNI ni headers de autorizacion.

`GlobalExceptionHandler` evita exponer stack traces al cliente y no registra el mensaje completo de `BusinessException` en logs.
