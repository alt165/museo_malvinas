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

## 4. Layout administrativo y navegacion principal

### Prompt utilizado

```text
Implementa el layout administrativo completo y la navegacion principal del frontend.

Usar como referencia:
- frontend/ARCHITECTURE.md
- sistema de roles ya implementado
- rutas previstas del sistema
- backend/API.md

Objetivo:
dejar lista la estructura visual y de navegacion antes de implementar modulos funcionales.

Requerimientos:

1. Mejorar AppShell con:
   - sidebar fijo o colapsable
   - topbar
   - area principal de contenido
   - responsive basico
   - estado visual de ruta activa

2. Mejorar Sidebar con navegacion a:
   - Dashboard
   - Objetos del museo
   - Inventario
   - Movimientos de inventario
   - Categorias
   - Depositantes
   - Veteranos
   - Actuaciones de veteranos
   - Exhibiciones
   - Perfil

3. Aplicar permisos visuales:
   - VIEWER puede ver modulos de consulta
   - ADMIN y OPERATOR pueden ver acciones operativas
   - no mostrar acciones no permitidas por rol

4. Mejorar Topbar:
   - nombre de usuario
   - roles
   - boton logout
   - estado de sesion

5. Crear paginas placeholder para todas las rutas principales:
   - /objetos
   - /objetos/nuevo
   - /objetos/[id]
   - /objetos/[id]/editar
   - /inventario
   - /movimientos-inventario
   - /categorias
   - /depositantes
   - /veteranos
   - /actuaciones-veteranos
   - /exhibiciones
   - /exhibiciones/nueva
   - /exhibiciones/[id]
   - /exhibiciones/[id]/editar
   - /perfil

6. Proteger todas las rutas internas con ProtectedRoute.

7. Crear componentes UI reutilizables si hace falta:
   - PageHeader
   - EmptyState
   - LoadingState
   - ErrorState

8. No consumir todavia endpoints reales.
9. No implementar CRUDs todavia.
10. No modificar logica de autenticacion salvo ajuste minimo necesario.

Ejecutar:
- npm run lint
- npm run build

Al finalizar:
- informar rutas creadas
- informar componentes reutilizables creados
- explicar como verificar navegacion con usuarios ADMIN, OPERATOR y VIEWER
```

### Objetivo del prompt

Completar la estructura visual y de navegacion del frontend administrativo antes de construir los modulos funcionales.

### Resultado esperado

- AppShell responsive con sidebar, topbar y contenido.
- Navegacion principal con estado activo.
- Acciones operativas visibles solo para `ADMIN` y `OPERATOR`.
- Placeholders para las rutas principales.
- Componentes reutilizables de encabezado y estados.

## 5. Modulo funcional de Objetos del Museo

### Prompt utilizado

```text
Implementa el modulo funcional de Objetos del Museo en el frontend.

Usar como referencia:
- frontend/ARCHITECTURE.md
- backend/API.md
- Swagger/OpenAPI
- modelos DTO reales del backend
- sistema de auth y permisos ya implementado

Objetivo:
permitir listar, ver detalle, crear y editar objetos del museo consumiendo la API real.

Requerimientos:

1. Crear modelos TypeScript para:
   - ObjetoMuseoRequestDTO
   - ObjetoMuseoResponseDTO
   - ApiErrorResponse si no existe o reutilizar el existente

2. Crear funciones API en:
   frontend/src/features/objetos/api

   Deben incluir:
   - listarObjetos
   - obtenerObjetoPorId
   - crearObjeto
   - actualizarObjeto
   - bajaLogicaObjeto si el backend lo permite

3. Usar TanStack Query para:
   - listado
   - detalle
   - creacion
   - actualizacion
   - invalidacion de queries

4. Implementar pagina:
   - /objetos

   Debe mostrar:
   - tabla con TanStack Table
   - numero de inventario
   - nombre
   - descripcion resumida
   - acciones ver/editar segun permisos
   - estado de loading
   - estado de error
   - empty state
   - boton "Nuevo objeto" solo para ADMIN/OPERATOR

5. Implementar pagina:
   - /objetos/[id]

   Debe mostrar detalle completo del objeto.

6. Implementar formulario reutilizable:
   - ObjetoMuseoForm

   Usar:
   - React Hook Form
   - Zod
   - validaciones alineadas con backend

7. Implementar:
   - /objetos/nuevo
   - /objetos/[id]/editar

8. Manejar errores del backend:
   - validationErrors
   - BusinessException
   - ResourceNotFoundException

9. Respetar permisos:
   - VIEWER solo lectura
   - ADMIN/OPERATOR pueden crear/editar/dar de baja

10. No implementar todavia inventario, categorias, depositantes ni exhibiciones salvo datos minimos necesarios para que el formulario compile.

11. Ejecutar:
   - npm run lint
   - npm run build

Al finalizar:
- informar archivos creados/modificados
- explicar como probar el modulo desde el navegador
- indicar endpoints consumidos
```

### Objetivo del prompt

Implementar el primer modulo funcional del frontend administrativo consumiendo la API real de objetos del museo.

### Resultado esperado

- Tipos TypeScript alineados con DTOs reales.
- API de feature para listar, obtener, crear, actualizar y dar de baja.
- Hooks TanStack Query con invalidacion.
- Listado con TanStack Table.
- Detalle, alta y edicion.
- Formulario con React Hook Form y Zod.
- Manejo de errores del backend y permisos por rol.
