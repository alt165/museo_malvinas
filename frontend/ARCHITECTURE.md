# Arquitectura Frontend

## Vision general

El frontend administrativo sera una aplicacion React con renderizado del lado del cliente inicialmente. La autenticacion se hara contra Keycloak usando un cliente propio del frontend:

```text
museo-frontend
```

El backend conserva su rol actual como OAuth2 Resource Server con el cliente:

```text
museo-backend
```

La separacion queda asi:

```text
Browser
  -> React CSR
  -> keycloak-js contra Keycloak
  -> access token en memoria
  -> API backend con Authorization Bearer
  -> Spring Security Resource Server
```

El frontend no valida tokens como fuente de verdad de seguridad. Solo usa el estado de autenticacion y los roles para experiencia de usuario, rutas y permisos visuales. La autorizacion definitiva queda en el backend.

## Stack recomendado

- React con TypeScript.
- Vite como bundler inicial recomendado para CSR.
- React Router para routing cliente.
- Tailwind CSS para estilos base.
- shadcn/ui como sistema de componentes.
- keycloak-js para autenticacion OIDC/OAuth2 con Keycloak.
- TanStack Query para cache, sincronizacion y mutaciones HTTP.
- TanStack Table para grillas, filtros, sorting, paginacion y seleccion.
- React Hook Form para formularios.
- Zod para validacion de formularios y schemas de entrada.
- Axios o Fetch encapsulado para cliente HTTP centralizado.
- OpenAPI/Swagger como fuente para alinear contratos TypeScript con DTOs del backend.

## Arquitectura interna

La aplicacion se organiza por capas practicas, no por tipo de archivo global. Las features concentran pantallas, componentes especificos, hooks y schemas del dominio que implementan.

```text
frontend/
  src/
    app/
      main.tsx
      router/
      providers/
      layouts/
    config/
      env.ts
      routes.ts
      permissions.ts
    auth/
      keycloak.ts
      auth-provider.tsx
      auth-hooks.ts
      protected-route.tsx
      role-guard.tsx
      session.ts
    api/
      http-client.ts
      query-client.ts
      errors.ts
      endpoints/
    features/
      dashboard/
      objetos/
      inventario/
      movimientos/
      categorias/
      depositantes/
      veteranos/
      actuaciones/
      exhibiciones/
      usuarios/
    shared/
      components/
      forms/
      tables/
      ui/
      hooks/
      lib/
      types/
    styles/
      globals.css
```

### Responsabilidades

- `app`: bootstrap, providers globales, router y layouts principales.
- `config`: variables de entorno parseadas, rutas internas y matriz de permisos.
- `auth`: integracion con Keycloak, estado de sesion y guards de rutas.
- `api`: cliente HTTP, integracion con TanStack Query y normalizacion de errores.
- `features`: modulos funcionales del museo.
- `shared`: componentes reutilizables, wrappers shadcn/ui, helpers y tipos comunes.

## Configuracion Keycloak

### Cliente frontend

El cliente `museo-frontend` debe configurarse en Keycloak como cliente publico para browser:

- Client authentication: deshabilitada.
- Standard flow: habilitado.
- Direct access grants: deshabilitado para frontend.
- Valid redirect URIs:
  - `http://localhost:5173/*` en local.
  - dominio real del frontend en ambientes desplegados.
- Web origins:
  - `http://localhost:5173` en local.
  - dominio real del frontend en ambientes desplegados.
- PKCE: requerido, preferentemente `S256`.

El cliente `museo-backend` se mantiene para el backend como resource server. Los roles pueden seguir viniendo desde `realm_access.roles` y/o `resource_access[museo-backend].roles`, porque el backend ya los convierte a `ROLE_*`.

## Flujo de autenticacion

### Inicio

1. El usuario abre el frontend.
2. La aplicacion inicializa `keycloak-js`.
3. Si no hay sesion activa, se redirige a Keycloak.
4. Keycloak autentica al usuario.
5. Keycloak redirige al frontend con el resultado del flujo OIDC.
6. `keycloak-js` completa el intercambio y deja los tokens disponibles en memoria.
7. La aplicacion carga perfil, roles y estado de sesion.
8. React Router habilita rutas segun rol.

No se implementa login propio con usuario y password dentro del frontend. La pantalla `/login`, si existe, debe ser solo una ruta tecnica o transicional que dispare `keycloak.login()`.

### Logout

1. El usuario ejecuta logout desde el layout principal.
2. El frontend limpia cache de TanStack Query.
3. Se llama a `keycloak.logout()` con redirect al origen del frontend.
4. Keycloak cierra la sesion SSO.

## Manejo de sesion

Los tokens no se guardan en `localStorage`. La estrategia inicial sera:

- Mantener access token y refresh token solo en memoria administrada por `keycloak-js`.
- Usar `check-sso` o login requerido segun la ruta inicial que se defina.
- Refrescar token antes de llamadas API mediante `updateToken`.
- Si el refresh falla, limpiar estado local y redirigir a Keycloak.
- No persistir tokens en storage propio.
- No registrar tokens en logs ni errores.

Implicacion: al refrescar la pagina, la aplicacion debe reconstruir la sesion consultando a Keycloak. Para que la experiencia sea fluida, Keycloak puede apoyarse en su propia cookie SSO. El frontend no debe copiar tokens a storage del browser.

## Manejo de API

El acceso HTTP debe estar encapsulado en un unico cliente:

```text
feature hook
  -> TanStack Query
  -> api client
  -> token provider Keycloak
  -> backend /api/**
```

### Reglas

- Todas las requests autenticadas agregan `Authorization: Bearer <access_token>`.
- Antes de cada request se intenta renovar el token si esta cerca de expirar.
- Los errores del backend se normalizan desde `ApiErrorResponse`.
- El header `X-Request-Id`, si viene en la respuesta, se conserva para soporte y diagnostico.
- TanStack Query maneja cache, loading, retry controlado e invalidaciones.
- Las mutaciones invalidan queries por clave de dominio.
- Los formularios usan React Hook Form y Zod antes de enviar DTOs al backend.
- Las tablas usan TanStack Table y delegan paginacion/filtros al backend cuando el volumen lo justifique.

### Politica de errores

- `401`: intentar refresh si aplica; si falla, redirigir a login.
- `403`: mostrar acceso denegado y mantener sesion.
- `400`: mapear errores de validacion a formularios cuando sea posible.
- `404`: mostrar estado vacio o recurso no encontrado segun contexto.
- `409` o errores de negocio: mostrar mensaje funcional del backend.
- `5xx`: mostrar error general e incluir `requestId` si esta disponible.

## Rutas y permisos

Roles actuales:

- `ADMIN`: acceso total.
- `OPERATOR`: gestion operativa sin administracion sensible.
- `VIEWER`: solo lectura.

Rutas principales:

```text
/
/dashboard
/objetos
/objetos/nuevo
/objetos/:id
/objetos/:id/editar
/inventario
/movimientos
/categorias
/depositantes
/veteranos
/veteranos/:id
/actuaciones
/exhibiciones
/exhibiciones/nueva
/exhibiciones/:id
/exhibiciones/:id/editar
/devoluciones/verificacion
/perfil
```

Las rutas de lectura admiten `ADMIN`, `OPERATOR` y `VIEWER`. Las rutas de alta, edicion, movimiento y verificacion admiten `ADMIN` y `OPERATOR`. Cualquier ruta futura de administracion de usuarios o configuracion debe quedar limitada a `ADMIN`.

## UI y formularios

shadcn/ui debe usarse como base para:

- Layout administrativo.
- Sidebar y topbar.
- Dialogs y sheets.
- Buttons, inputs, selects, comboboxes y date pickers.
- Toasts o sonner para feedback.
- Dropdowns de usuario y acciones.
- Estados vacios, loading y errores.

React Hook Form y Zod deben modelar cada formulario por feature. Los schemas Zod del frontend validan UX y consistencia antes de enviar, pero no reemplazan la validacion del backend.

## Tablas

TanStack Table debe centralizar:

- Column definitions por feature.
- Sorting.
- Filtering.
- Visibility de columnas.
- Row selection cuando aplique.
- Acciones por fila segun permisos.
- Paginacion cliente o servidor segun endpoint y volumen.

Para entidades principales como objetos, inventario, movimientos y exhibiciones, se recomienda preparar desde el inicio una API de tabla compatible con paginacion y filtros, aunque la primera version pueda consumir listas simples.

## Dependencias recomendadas

### Runtime

- `@vitejs/plugin-react`
- `vite`
- `typescript`
- `react`
- `react-dom`
- `react-router-dom`
- `keycloak-js`
- `@tanstack/react-query`
- `@tanstack/react-table`
- `react-hook-form`
- `zod`
- `@hookform/resolvers`
- `axios` o cliente `fetch` propio
- `tailwindcss`
- `class-variance-authority`
- `clsx`
- `tailwind-merge`
- `lucide-react`
- `sonner`
- dependencias base de shadcn/ui segun componentes instalados

### Desarrollo y calidad

- `eslint`
- `typescript-eslint`
- `prettier`
- `vitest`
- `@testing-library/react`
- `@testing-library/user-event`
- `jsdom`
- `msw` para mocks HTTP
- `playwright` para smoke/e2e cuando existan flujos completos

## Variables de entorno

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=museo
VITE_KEYCLOAK_CLIENT_ID=museo-frontend
```

Estas variables no son secretas. El cliente frontend es publico y no debe incluir client secret.

## Decisiones tomadas

- Frontend CSR inicial.
- Cliente Keycloak propio: `museo-frontend`.
- Backend sigue como resource server con `museo-backend`.
- Autenticacion con `keycloak-js`.
- Tokens solo en memoria, sin `localStorage`.
- TanStack Query como capa de estado servidor.
- TanStack Table para tablas administrativas.
- React Hook Form y Zod para formularios.
- shadcn/ui como base de componentes.
- Autorizacion real en backend; guards frontend solo para UX y navegacion.

## Riesgos y pendientes

- Confirmar si los roles quedaran en realm roles, en client roles de `museo-backend`, o en ambos.
- Agregar `museo-frontend` al realm local importado por Docker.
- Revisar CORS del backend para incluir el origen real del frontend.
- Definir si el arranque de la app sera `login-required` o `check-sso` con rutas publicas minimas.
- Validar si los endpoints necesitan paginacion y filtros server-side antes de construir tablas de alto volumen.
- Definir estrategia de generacion o mantenimiento manual de tipos TypeScript desde OpenAPI.
