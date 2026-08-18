# licitaciones-frontend

Frontend (React 19 + Vite + JavaScript) para la automatización de procesos
de licitación. Consume la API REST expuesta por `licitaciones-backend`
(`/api/licitaciones`, `/api/auth`, `/api/usuarios`), sin ningún endpoint
adicional inventado.

## Tecnologías

- React 19 + React Router 7 (rutas)
- Axios (HTTP)
- Tailwind CSS v4 (`@tailwindcss/vite`)
- JavaScript (sin TypeScript)

## Requisitos

- Node.js 18+ (probado con Node 26)
- El backend `licitaciones-backend` corriendo en `http://localhost:8080`
  (o la URL que definas en `VITE_API_BASE_URL`), con CORS habilitado hacia
  `http://localhost:5173` (valor por defecto de `CORS_ALLOWED_ORIGIN` en el backend)

## Configuración

Copia `.env.example` a `.env` y ajusta si el backend no corre en el valor por defecto:

```bash
cp .env.example .env
```

```
VITE_API_BASE_URL=http://localhost:8080
```

**Producción (Vercel)**: `.env` es local y nunca se sube al repo (ver
`.gitignore`); Vercel no lo lee. En Vercel, la variable se configura en
**Project Settings → Environment Variables**, con la URL publica que da
Render despues del deploy del backend (ver el README de
`licitaciones-backend`, sección "Despliegue en Render"), por ejemplo:

```
VITE_API_BASE_URL=https://<nombre-del-servicio>.onrender.com
```

## Ejecución

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # genera dist/ para producción
npm run preview  # sirve dist/ localmente para verificar el build
npm run lint
```

## Estructura

```
src/
├── api/              # llamadas HTTP crudas (axiosConfig con interceptores de
│                      # auth, authApi, usuariosApi, licitacionesApi)
├── services/         # capa de servicio: normaliza datos y errores (authService,
│                      # usuarioService, licitacionService)
├── context/          # AuthContext (sesión: login/logout/usuario/token)
├── hooks/            # useAuth, useCrearUsuario, useProcesos, useProceso, useCrearProceso, useAnalisis
├── components/        # Button, Spinner, Badge, Modal, Table, ProgressBar,
│                      # FileDropzone, ErrorAlert, Layout, ProtectedRoute, AdminRoute
├── pages/             # Login, Dashboard, CrearProceso, AnalizarDocumentos,
│                      # Resultado, DetalleProceso, Usuarios, NotFound
└── utils/             # formatters, constants, errorHandler, downloadFile,
                       # trazabilidad (cruce fuente ↔ ficha técnica), authStorage
```

## Autenticación

Toda la aplicación exige sesión (JWT contra `POST /api/auth/login` del
backend); no hay registro público ni pantalla de "crear cuenta" — el único
usuario es el administrador que se inicializa desde el backend (ver el
README de `licitaciones-backend`, sección "Autenticación (JWT)").

- **`/login`** es la única ruta pública. Cualquier otra ruta está envuelta
  en `<ProtectedRoute />` (`components/ProtectedRoute`): sin sesión,
  redirige a `/login` recordando a dónde iba el usuario para volver ahí
  después de autenticarse.
- **`context/AuthContext.jsx`** (consumido vía `hooks/useAuth.js`) centraliza
  `login()`, `logout()`, `user` (`{ username, role }`) y `token`.
- **`utils/authStorage.js`** guarda el token y los datos del usuario en
  `localStorage`, en tres claves separadas (`token`, `username`, `role`).
  Es una elección deliberada (el cliente HTTP es Axios puro, sin manejo de
  cookies/CSRF) con un riesgo conocido: cualquier XSS en la página podría
  leer la sesión completa. El archivo documenta la mitigación aplicada y el
  camino de migración a cookies `httpOnly` si hiciera falta.
- **Roles y `/usuarios`**: un usuario es `ADMIN` o `USER`. Solo un `ADMIN` ve
  el link "Usuarios" en el header y puede entrar a `/usuarios`
  (`components/AdminRoute`, que redirige a `/` si `user.role !== "ADMIN"`)
  para crear usuarios nuevos (`POST /api/usuarios`). Esto es solo
  conveniencia de UI: la seguridad real la aplica el backend
  (`hasRole("ADMIN")`), que responde `403` si un `USER` llama el endpoint
  directamente sin pasar por esta pantalla.
- **`api/axiosConfig.js`** centraliza la autenticación de TODAS las llamadas
  vía dos interceptores de Axios: agrega `Authorization: Bearer <token>` a
  cada request, y si el backend responde `401` (token ausente/expirado/
  inválido, y la petición no era el login) limpia la sesión y redirige a
  `/login?sessionExpired=1` — así ninguna página queda "atrapada" reintentando
  peticiones que van a seguir fallando.
- **Logout** (botón en `Layout`) solo borra la sesión del navegador; no
  toca procesos ni ningún otro dato del backend.

## Flujo de la aplicación

1. **Dashboard** (`/`): historial de procesos (`GET /api/licitaciones`).
2. **Nuevo proceso** (`/procesos/nuevo`): crea el proceso (`POST /api/licitaciones`)
   y redirige a la carga de documentos.
3. **Analizar documentos** (`/procesos/:id/analizar`): selección de carpeta/archivos
   (drag&drop o `webkitdirectory`) y disparo del análisis
   (`POST /api/licitaciones/:id/analizar`, sincrónico — puede tardar varios minutos).
4. **Resultado** (`/procesos/:id/resultado`): ficha técnica, documentación y
   trazabilidad (`GET /api/licitaciones/:id/resultado`).
5. **Detalle de proceso** (`/procesos/:id`): estado, fechas y descarga del Excel
   (`GET /api/licitaciones/:id`, `GET /api/licitaciones/:id/excel`).
6. **Usuarios** (`/usuarios`, solo ADMIN): formulario para crear un usuario
   nuevo (`POST /api/usuarios`). No hay listado/edición/borrado de usuarios,
   solo creación.

## Notas importantes sobre el contrato con el backend

- `POST /{id}/analizar` **no** devuelve la ficha técnica/documentación/fuentes,
  solo el `ProcesoResponseDTO` actualizado. El detalle del análisis se obtiene
  siempre con `GET /{id}/resultado` (por eso `useAnalisis` encadena ambas
  llamadas tras un análisis exitoso).
- `FuenteDTO` (trazabilidad) solo expone `campo`, `archivo` y `pagina` — **no**
  incluye el valor/dato encontrado. La columna "Información encontrada" de la
  pantalla de Resultado hace un cruce best-effort contra `FichaTecnicaDTO`
  cuando el nombre del campo coincide (ver `utils/trazabilidad.js`); si no hay
  coincidencia, se muestra explícitamente "No disponible" en vez de inventar
  un valor.
- No hay endpoint de eliminación de procesos en el backend actual, por lo
  que el frontend tampoco lo implementa.
- No hay registro de usuarios ni login social (Google/Facebook): el único
  endpoint de autenticación es `POST /api/auth/login` (ver sección
  "Autenticación" más arriba).
