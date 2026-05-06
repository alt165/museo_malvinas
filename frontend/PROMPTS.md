# Prompts del Frontend Administrativo

Este archivo registra las consignas usadas para diseñar y construir el frontend administrativo del sistema de inventario del museo.

## 1. Arquitectura inicial del frontend administrativo

### Prompt utilizado

```text
Vamos a iniciar el desarrollo del frontend administrativo para el sistema de inventario del museo.

Usar como referencia del backend:
- README.md
- ARCHITECTURE.md
- DOMAIN.md
- API.md
- SECURITY.md
- SETUP.md
- Swagger/OpenAPI disponible
- endpoints reales del backend
- roles definidos en Keycloak: ADMIN, OPERATOR, VIEWER

Objetivo:
diseñar la arquitectura inicial del frontend antes de generar código.

Requerimientos:

1. Proponer stack frontend recomendado.
   Preferencia:
   - Next.js
   - TypeScript
   - Tailwind CSS
   - React Hook Form
   - Zod
   - cliente HTTP centralizado

2. Definir estructura de carpetas profesional.

3. Definir rutas principales del sistema:
   - login
   - dashboard
   - objetos del museo
   - detalle de objeto
   - alta/edición de objeto
   - inventario
   - movimientos de inventario
   - categorías
   - depositantes
   - veteranos
   - actuaciones de veteranos
   - exhibiciones
   - detalle de exhibición
   - verificación de devolución
   - usuarios/perfil si corresponde

4. Definir layout principal:
   - sidebar
   - topbar
   - contenido
   - estado de sesión
   - logout

5. Definir integración con Keycloak:
   - login externo
   - almacenamiento seguro del token
   - envío de Authorization Bearer
   - control de rutas por rol

6. Definir permisos por rol:
   - ADMIN: acceso total
   - OPERATOR: gestión operativa
   - VIEWER: solo lectura

7. Definir estrategia para consumir la API:
   - cliente HTTP
   - manejo centralizado de errores
   - refresh/reautenticación si aplica
   - uso de variables de entorno

8. Definir modelos TypeScript basados en los DTOs del backend.

9. No generar código todavía.
10. No crear componentes todavía.
11. Solo devolver un documento técnico de arquitectura frontend.

Al finalizar:
- listar decisiones tomadas
- listar pantallas necesarias
- listar riesgos o dudas antes de implementar el frontend tiene que crearse en la carpeta frontend y necesito que todos los prompts que te pase se vayan almacenando en un archivo PROMPTS.md en la carpeta frontend
```

### Objetivo del prompt

Definir la arquitectura inicial del frontend administrativo antes de implementar codigo, alineando rutas, permisos, autenticacion, consumo de API y modelos TypeScript con el backend existente.

### Resultado esperado

- Documento tecnico de arquitectura frontend.
- Decisiones de stack y estructura de carpetas.
- Rutas y pantallas principales.
- Estrategia de integracion con Keycloak.
- Politica de permisos por rol.
- Estrategia de cliente HTTP y manejo de errores.
- Modelos TypeScript derivados de DTOs reales del backend.
- Sin componentes ni codigo funcional todavia.

## 2. Refinamiento de arquitectura frontend con cliente Keycloak propio

### Prompt utilizado

```text
Refina la arquitectura frontend considerando las siguientes decisiones:

- El frontend tendra su propio cliente Keycloak:
  museo-frontend
- El backend mantiene museo-backend como resource server.
- El frontend usara CSR inicialmente.
- Se usara keycloak-js para autenticacion.
- No se usara localStorage para tokens.
- Se usara:
  - shadcn/ui
  - TanStack Query
  - TanStack Table
  - React Hook Form
  - Zod

Actualizar:
- arquitectura
- estructura de carpetas
- flujo de autenticacion
- manejo de sesion
- manejo de API
- dependencias recomendadas

No generar codigo todavia.
```

### Objetivo del prompt

Actualizar la arquitectura del frontend administrativo para separar correctamente el cliente publico del browser (`museo-frontend`) del resource server del backend (`museo-backend`), manteniendo CSR inicial y autenticacion con `keycloak-js`.

### Resultado esperado

- Documento `ARCHITECTURE.md` del frontend actualizado.
- Estructura de carpetas orientada a features.
- Flujo de autenticacion con Keycloak externo.
- Manejo de sesion sin persistir tokens en `localStorage`.
- Cliente API centralizado con TanStack Query.
- Dependencias recomendadas para UI, formularios, validacion y tablas.
- Sin componentes ni codigo funcional todavia.

## 3. Implementacion de autenticacion real con Keycloak

### Prompt utilizado

```text
Implementa autenticacion real con Keycloak en el frontend.

Usar como referencia:
- frontend/ARCHITECTURE.md
- backend/SECURITY.md
- backend/API.md
- variables de entorno de frontend
- roles: ADMIN, OPERATOR, VIEWER

Requerimientos:

1. Configurar keycloak-js usando:
   - NEXT_PUBLIC_KEYCLOAK_URL
   - NEXT_PUBLIC_KEYCLOAK_REALM
   - NEXT_PUBLIC_KEYCLOAK_CLIENT_ID

2. Implementar AuthProvider real:
   - inicializar Keycloak
   - login
   - logout
   - estado authenticated/loading
   - usuario actual
   - roles del token
   - token en memoria, nunca localStorage

3. Integrar el token con el cliente API centralizado:
   - enviar Authorization: Bearer <token>
   - refrescar token antes de requests si esta por expirar
   - redirigir a login si no se puede refrescar

4. Crear helpers:
   - hasRole
   - hasAnyRole
   - canWrite
   - canRead

5. Proteger rutas:
   - usuarios no autenticados -> login
   - VIEWER solo lectura
   - ADMIN y OPERATOR con acciones operativas

6. Crear paginas minimas:
   - /login
   - /dashboard
   - /unauthorized

7. Actualizar layout para mostrar:
   - usuario autenticado
   - roles
   - boton logout

8. No implementar todavia modulos funcionales como objetos, inventario o exhibiciones.

9. No guardar tokens en localStorage ni sessionStorage.

10. Ejecutar:
   - npm run lint
   - npm run build

Al finalizar:
- informar archivos modificados
- explicar como probar login con Keycloak local
- indicar variables necesarias en .env.local. Siempre guarda el prompt en el archivo PROMPTS.md
```

### Objetivo del prompt

Implementar la autenticacion real del frontend administrativo con `keycloak-js`, manteniendo tokens solo en memoria y conectando el token al cliente API centralizado.

### Resultado esperado

- `AuthProvider` real con login, logout, loading, usuario y roles.
- Helpers de permisos para lectura y escritura.
- Cliente API con bearer token y refresh previo a requests.
- Rutas minimas `/login`, `/dashboard` y `/unauthorized`.
- Layout con usuario, roles y logout.
- Sin modulos funcionales todavia.
