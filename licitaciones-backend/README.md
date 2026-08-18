# licitaciones-backend

Backend (Spring Boot 3 / Java 17) para la automatizacion del analisis de
procesos de licitacion. Expone una API REST para el frontend React,
delega el analisis de documentos a un flujo de **n8n**, persiste el
resultado y genera el Excel final a partir de una plantilla oficial.

## Arquitectura

Arquitectura por capas (no hexagonal), organizada en paquetes por
responsabilidad:

```
com.licitaciones.backend
├── auth         # Login JWT: AuthController, AuthService, JwtService, JwtAuthenticationFilter
├── security     # SecurityConfig (rutas publicas/protegidas + hasRole) + JwtAuthenticationEntryPoint (401) + JwtAccessDeniedHandler (403)
├── config       # CORS, RestTemplate, @ConfigurationProperties (n8n, excel, cors, jwt, admin inicial)
├── controller   # API REST (sin logica de negocio), incluye UsuarioController (POST /api/usuarios)
├── service      # Orquestacion (LicitacionService, N8nService, ExcelService, UsuarioService)
├── client       # Cliente HTTP hacia n8n (N8nClient)
├── dto
│   ├── request  # Datos de entrada validados con Jakarta Validation
│   └── response # Datos de salida (incluye el contrato JSON de n8n)
├── entity       # Entidades JPA (ProcesoLicitacion, ResultadoAnalisis, Usuario, Rol)
├── repository   # Spring Data JPA
├── exception    # Excepciones de negocio + GlobalExceptionHandler
└── util         # ExcelUtil (relleno de plantillas sin romper estilos)
```

## Requisitos

- Java 17
- Maven (o usar el wrapper `./mvnw` / `mvnw.cmd` incluido, no requiere instalacion previa)
- MySQL 8.x en ejecucion
- (Opcional para el flujo completo) una instancia de n8n con el webhook de analisis configurado

## Configuracion

Todo se configura via variables de entorno (con valores por defecto para
desarrollo local) en `src/main/resources/application.properties`:

| Variable | Descripcion | Default |
|---|---|---|
| `PORT` | Puerto HTTP. En Render lo inyecta la plataforma; en local, sin definir, usa 8080 | `8080` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexion MySQL | `127.0.0.1`, `3306`, `licitaciones_db` |
| `DB_USERNAME` | Usuario MySQL | `root` |
| `DB_PASSWORD` | Password MySQL. **Sin default**: no hay ninguna contrasena real en el codigo fuente, hay que definirla siempre (tambien en local) | *(vacio: la conexion falla si no se define)* |
| `DB_SSL_MODE` | Modo TLS de MySQL Connector/J (`sslMode`). `PREFERRED` negocia TLS si el servidor lo ofrece y si no, sigue sin TLS (compatible con MySQL local con o sin TLS). En produccion contra un proveedor que exige TLS (ej. Aiven), usar `REQUIRED` | `PREFERRED` |
| `JPA_DDL_AUTO` | Estrategia de esquema (`update` en dev) | `update` |
| `N8N_BASE_URL` | URL base de la instancia de n8n | `http://localhost:5678` |
| `N8N_WEBHOOK_ANALIZAR` | Path del webhook de analisis | `/webhook/licitaciones/analizar` |
| `N8N_CONNECT_TIMEOUT_MS`, `N8N_READ_TIMEOUT_MS` | Timeouts hacia n8n | `5000`, `180000` |
| `EXCEL_TEMPLATE_PATH` | Ruta (classpath) de la plantilla | `templates/excel/plantilla_licitacion.xlsx` |
| `EXCEL_OUTPUT_DIR` | Carpeta donde se guardan los Excel generados | `./generated/excel` |
| `CORS_ALLOWED_ORIGIN` | Origen de desarrollo permitido (frontend Vite) | `http://localhost:5173` |
| `FRONTEND_URL` | Origen de **produccion** adicional (ej. el deploy de Vercel), se suma al de desarrollo sin reemplazarlo (ver [Despliegue en Render](#despliegue-en-render-docker)) | *(vacio: no se agrega ningun origen extra)* |
| `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE` | Limites de carga de documentos | `50MB`, `300MB` |
| `JWT_SECRET` | Clave con la que se firman los JWT. **Cambiar siempre en produccion/QA** (el default solo es valido para desarrollo local) | valor generado, ver `application.properties` |
| `JWT_EXPIRATION` | Vigencia del token en milisegundos | `86400000` (24h) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Credenciales del **unico** usuario administrador, usadas solo la primera vez que arranca la app (ver [Autenticacion](#autenticacion-jwt)) | `admin`, *(vacio: no crea admin)* |

Crea la base de datos (o deja que `createDatabaseIfNotExist=true` la cree, si el
usuario de MySQL tiene permiso):

```sql
CREATE DATABASE IF NOT EXISTS licitaciones_db CHARACTER SET utf8mb4;
```

## Ejecucion del proyecto

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

`DB_PASSWORD` es obligatoria siempre (tambien en local): no hay ninguna
contrasena por defecto en el codigo. Ejemplo Windows PowerShell:

```powershell
$env:DB_PASSWORD = "mi-password"
$env:N8N_BASE_URL = "https://mi-n8n.miempresa.com"
mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080/api/licitaciones`.

Para generar el JAR ejecutable:

```bash
mvnw.cmd clean package
java -jar target/licitaciones-backend-0.0.1-SNAPSHOT.jar
```

## Endpoints

`POST /api/auth/login` es el **unico** endpoint publico de toda la API (ver
[Autenticacion](#autenticacion-jwt)); todos los demas exigen
`Authorization: Bearer <token>`. `POST /api/usuarios` ademas exige rol
`ADMIN` (`403` si el token es de un usuario `USER`).

| Metodo | Ruta | Descripcion |
|---|---|---|
| `POST` | `/api/auth/login` | **Publico.** Autentica (`{ "username": "...", "password": "..." }`) y devuelve el JWT |
| `POST` | `/api/usuarios` | **Solo ADMIN.** Crea un usuario (`{ "username": "...", "password": "...", "rol": "ADMIN"\|"USER" }`). `409` si el username ya existe, `403` si quien llama no es ADMIN |
| `POST` | `/api/licitaciones` | Crea un proceso (`{ "nombreProceso": "..." }`) |
| `GET`  | `/api/licitaciones` | Lista el historial de procesos |
| `GET`  | `/api/licitaciones/{id}` | Detalle de un proceso |
| `POST` | `/api/licitaciones/{id}/analizar` | Envia los documentos (`multipart/form-data`, campo `documentos`) a n8n, guarda el resultado y genera el Excel |
| `GET`  | `/api/licitaciones/{id}/estado` | Estado actual (`CREADO`, `ANALIZANDO`, `COMPLETADO`, `ERROR`) |
| `GET`  | `/api/licitaciones/{id}/resultado` | Ultimo resultado de analisis persistido (ficha tecnica, documentacion y trazabilidad de fuentes). `409` si el proceso aun no ha sido analizado |
| `GET`  | `/api/licitaciones/{id}/excel` | Descarga `Licitacion_Final.xlsx` |

### Ejemplo de flujo con `curl`

```bash
# 0. Login (unico endpoint publico) -> guarda el token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"mi-password\"}" | jq -r .token)

# 1. Crear proceso (requiere el token en TODO lo demas)
curl -X POST http://localhost:8080/api/licitaciones \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"nombreProceso\":\"Licitacion ejemplo\"}"
# -> { "id": 1, "estado": "CREADO", ... }

# 2. Iniciar analisis (con documentos del proceso)
curl -X POST http://localhost:8080/api/licitaciones/1/analizar \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentos=@EstudioPrevio.pdf" \
  -F "documentos=@PliegoCondiciones.pdf"

# 3. Consultar estado
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/licitaciones/1/estado

# 4. Descargar el Excel final
curl -OJ -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/licitaciones/1/excel
```

## Autenticacion (JWT)

La aplicacion es privada por completo: **no hay registro publico**. El
unico endpoint sin autenticar es `POST /api/auth/login`; todo lo demas
(`/api/licitaciones/**`, `/api/health/**`, `/api/usuarios`, etc.) exige el
header `Authorization: Bearer <token>` o responde `401`.

> ⚠️ **Las contrasenas se guardan y se comparan en TEXTO PLANO** (columna
> `usuarios.password`), sin BCrypt ni ningun otro hashing. Es una decision
> explicita de este proyecto (simplicidad por sobre seguridad de
> contrasenas), no un descuido: `auth.AuthService` hace literalmente
> `usuario.getPassword().equals(passwordRecibida)`. Si en algun momento se
> quiere volver a hashear, el unico lugar que cambiaria es ese `.equals(...)`
> (mas como se generan/guardan las contrasenas en `UsuarioService`); el JWT,
> `JwtAuthenticationFilter` y el resto de `SecurityConfig` no dependen de
> esto para nada.

### Roles

Un usuario tiene un `rol`: `ADMIN` o `USER` (columna `usuarios.rol`, texto
libre — ver la nota de `entity.Usuario` sobre por que ya no es un ENUM
nativo de MySQL). El JWT lleva el rol como claim `role`, y
`JwtAuthenticationFilter` lo traduce a la autoridad de Spring Security
`ROLE_<rol>`. Hoy el unico endpoint con una regla de rol especifica es
`POST /api/usuarios` (`hasRole("ADMIN")`); el resto de rutas protegidas solo
exige estar autenticado, sin importar el rol.

### Crear el primer (y unico) administrador

No existe `/api/auth/register` ni ninguna pantalla de registro publica. El
mecanismo para tener el primer usuario es definir
`ADMIN_USERNAME`/`ADMIN_PASSWORD` como variables de entorno **antes** de
arrancar la aplicacion por primera vez:

```powershell
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "una-clave-solo-para-este-arranque"
mvnw.cmd spring-boot:run
```

Al arrancar, `config.AdminUsuarioInitializer` crea ese usuario (rol
`ADMIN`, contrasena tal cual, sin hashear) si todavia no existe uno con ese
`username`; si ya existe, no lo toca (no resetea la contrasena en cada
reinicio). Si `ADMIN_PASSWORD` no esta definido, no se crea ningun usuario:
la aplicacion arranca igual, simplemente nadie puede loguearse todavia
hasta que definas esas variables. Puedes quitarlas del entorno despues del
primer arranque exitoso.

Para el resto de usuarios (otros ADMIN o USER), usa `POST /api/usuarios`
(ver abajo) autenticado como un ADMIN existente — no hace falta reiniciar
la aplicacion ni tocar variables de entorno.

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"...\"}"
```

Correcto (`200`):

```json
{ "success": true, "message": "Login exitoso", "token": "eyJhbGciOiJIUzUxMiJ9...", "username": "admin", "role": "ADMIN" }
```

Incorrecto (`401`, mensaje siempre generico: nunca revela si el usuario
existe o si fue la contrasena):

```json
{ "success": false, "message": "Credenciales inválidas" }
```

### Crear usuarios (solo ADMIN)

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_DE_UN_ADMIN" \
  -d "{\"username\":\"TatianaSofgic\",\"password\":\"LicitacionesTati#2026\",\"rol\":\"USER\"}"
```

Creado (`201`), `activo` siempre queda en `true`, nunca se devuelve la contrasena:

```json
{ "id": 3, "username": "TatianaSofgic", "rol": "USER", "activo": true }
```

Username duplicado (`409`):

```json
{ "success": false, "message": "El usuario ya existe" }
```

Sin token: `401`. Con token de un usuario `USER` (no `ADMIN`): `403`.

### Detalles de implementacion

- El JWT lleva `sub` (username), `role` y las fechas estandar `iat`/`exp`.
  Nunca lleva la contrasena ni ningun otro dato sensible.
- `JwtAuthenticationFilter` valida el token y reconstruye la autenticacion
  **solo con esos claims**, sin volver a consultar MySQL en cada request.
- `JwtAccessDeniedHandler` (403) y `JwtAuthenticationEntryPoint` (401)
  responden ambos el mismo contrato `ErrorResponseDTO` que usa
  `GlobalExceptionHandler` para el resto de la API.
- El frontend guarda el token (y ahora tambien username/role, en claves
  separadas: `token`, `username`, `role`) en `localStorage` (ver
  `licitaciones-frontend/src/utils/authStorage.js`, que documenta el riesgo
  de XSS de esa eleccion y el camino de migracion a cookies `httpOnly` si
  hiciera falta mas adelante) y lo agrega automaticamente a cada peticion
  via un interceptor de Axios; si el backend responde `401` (token
  ausente/expirado/invalido), el interceptor limpia la sesion y redirige a
  `/login`.

## Contrato JSON esperado desde n8n

El webhook de analisis debe responder con este formato (ver
`dto.response.N8nAnalisisResponseDTO`):

```json
{
  "fichaTecnica": {
    "entidad": "Alcaldia de Bogota",
    "numeroProceso": "LP-001-2026",
    "objeto": "Servicio tecnologico",
    "presupuesto": 50000000,
    "plazo": "6 meses"
  },
  "documentacion": [
    { "nombre": "Certificado Camara Comercio", "obligatorio": true }
  ],
  "fuentes": [
    { "campo": "Presupuesto", "archivo": "EstudioPrevio.pdf", "pagina": 15 }
  ]
}
```

### Contrato de `data.polizas` (Polizas / Garantias)

Para que el frontend pueda mostrar el archivo y la pagina de cada poliza (y
no solo el porcentaje), n8n debe enviar cada uno de los seis tipos como un
objeto `{ "valor": ..., "archivo": ..., "pagina": ... }`, no como el numero
"pelado" del contrato anterior:

```json
"polizas": {
  "seriedad": { "valor": 10, "archivo": "Pliego_Condiciones.pdf", "pagina": 42 },
  "cumplimiento": { "valor": 10, "archivo": "Pliego_Condiciones.pdf", "pagina": 42 },
  "buenManejoAnticipo": { "valor": 0, "archivo": "Pliego_Condiciones.pdf", "pagina": 43 },
  "pagoSalarios": { "valor": 5, "archivo": "Pliego_Condiciones.pdf", "pagina": 43 },
  "estabilidad": { "valor": 0, "archivo": "Pliego_Condiciones.pdf", "pagina": 43 },
  "calidad": { "valor": 10, "archivo": "Pliego_Condiciones.pdf", "pagina": 43 }
}
```

- `valor`: porcentaje exigido (entero). `0` es valido (poliza no exigida) y
  debe enviarse igual, nunca omitirse.
- `archivo`/`pagina`: trazabilidad de donde se encontro el requisito. Si n8n
  no puede determinarlos, mejor omitirlos (o enviar `null`) que inventarlos;
  el frontend muestra "No disponible" en ese caso.
- Compatibilidad: `dto.response.PolizaDTO` (via `PolizaFlexibleDeserializer`)
  sigue aceptando el contrato viejo (`"seriedad": 10`), pero en ese caso
  `archivo`/`pagina` quedan `null` porque el numero "pelado" no trae esa
  informacion.

## Plantilla Excel

Ver [`src/main/resources/templates/excel/README.md`](src/main/resources/templates/excel/README.md)
para el detalle de la estructura esperada y como reemplazar el placeholder
incluido por la plantilla oficial.

## Despliegue en Render (Docker)

El `Dockerfile` (raiz de `licitaciones-backend/`) es multi-stage: compila
con Maven Wrapper sobre `eclipse-temurin:17-jdk-jammy` y la imagen final
corre solo con `eclipse-temurin:17-jre-jammy` (sin JDK/Maven, usuario sin
privilegios). Los tests se saltan en el build de la imagen (`-DskipTests`):
varios requieren un MySQL real en `127.0.0.1` que no existe dentro del
contenedor; correr `./mvnw test` sigue siendo el paso de CI/desarrollo.

### Configuracion del servicio en Render

| Campo | Valor |
|---|---|
| Language / Runtime | `Docker` |
| Root Directory | `licitaciones-backend` |
| Dockerfile Path | `licitaciones-backend/Dockerfile` (relativo a la raiz del repo, no al Root Directory) |
| Build Command | No aplica: con `Language: Docker`, Render construye la imagen a partir del `Dockerfile` (sus propios `RUN`), no de un Build Command manual |
| Start Command | No aplica: el `ENTRYPOINT` del `Dockerfile` (`java -jar app.jar`) es el que arranca el contenedor |

### Puerto

No hay que configurar nada a mano: Render inyecta la variable de entorno
`PORT` en el contenedor, y `server.port=${PORT:8080}` (ver
`application.properties`) hace que Spring Boot escuche ahi automaticamente.
En local, sin `PORT` definida, sigue usando 8080 como siempre.

### Variables de entorno a crear en Render

Obligatorias para que el servicio arranque:

| Variable | Valor |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Datos de conexion del MySQL de produccion (ej. Aiven) |
| `DB_USERNAME`, `DB_PASSWORD` | Credenciales de ese MySQL |
| `DB_SSL_MODE` | `REQUIRED` si el proveedor exige TLS (Aiven lo exige) |
| `JWT_SECRET` | Un valor propio y aleatorio (32+ bytes) — **no** reutilizar el default de desarrollo |

Recomendadas / segun el caso (todas tienen default si no se definen, ver la
tabla de [Configuracion](#configuracion) mas arriba):

| Variable | Para que |
|---|---|
| `FRONTEND_URL` | Dominio del frontend en Vercel (ej. `https://mi-frontend.vercel.app`), se suma a `http://localhost:5173` sin reemplazarlo |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Solo para el primer arranque, crea el usuario administrador inicial (ver [Autenticacion](#autenticacion-jwt)); se pueden quitar despues |
| `N8N_BASE_URL`, `N8N_WEBHOOK_ANALIZAR` | Si la instancia de n8n de produccion es distinta a la de desarrollo |
| `JPA_DDL_AUTO` | Dejar en `update` (default) a menos que se maneje el esquema de otra forma |

⚠️ El workflow de n8n que recibe `N8N_WEBHOOK_ANALIZAR` tiene que estar
**activado** (toggle arriba a la derecha del editor, no solo guardado): con
el workflow inactivo, n8n devuelve 404 "not registered" y el backend lo
traduce en `502 Bad Gateway` al frontend (ver el comentario en
`application.properties`, ya documentado desde antes de este cambio, no es
nuevo).

**Ejemplo con Aiven** (sustituye `DB_PASSWORD` por la contrasena real, nunca
la pongas en el codigo/repo):

```
DB_HOST=mysql-1d20ba80-cristiandres1317-5d38.l.aivencloud.com
DB_PORT=21874
DB_NAME=defaultdb
DB_USERNAME=avnadmin
DB_PASSWORD=<la contrasena real, solo en Render>
DB_SSL_MODE=REQUIRED
```

⚠️ Sobre `createDatabaseIfNotExist=true` (parte del `spring.datasource.url`
actual, sin tocar): si el usuario de MySQL en produccion no tiene permiso
para crear bases de datos, la conexion inicial va a fallar. Con Aiven no es
un problema: `defaultdb` ya existe de entrada en cualquier servicio Aiven,
asi que esa clausula simplemente no hace nada (la base ya esta ahi).

⚠️ Sin Flyway ni otra herramienta de migraciones: el esquema (tablas
`proceso_licitacion`, `resultado_analisis`, `usuarios`) lo crea/actualiza
Hibernate solo via `spring.jpa.hibernate.ddl-auto=update`, igual en local
que en Aiven — no se agrego Flyway porque el proyecto no lo usaba y hacerlo
es un cambio de arquitectura por si solo, fuera del alcance de "conectar
todo a produccion".

⚠️ `app.excel.output-dir` (por defecto `./generated/excel`) escribe en el
filesystem del contenedor, que en Render **no es persistente**: los Excel
generados no sobreviven un redeploy/reinicio. Esto no cambia nada del
comportamiento actual (no se toco), solo es una limitacion a tener en
cuenta al operar en Render.

### URL del backend para el frontend (Vercel)

Una vez desplegado, Render asigna una URL del tipo
`https://<nombre-del-servicio>.onrender.com` (visible en el dashboard del
servicio). Esa es la URL que va en `VITE_API_BASE_URL` del frontend en
Vercel — reemplaza al `http://localhost:8080` usado en desarrollo.

## Notas de diseno

- Inyeccion de dependencias por constructor (`@RequiredArgsConstructor` de Lombok) en toda la app.
- DTOs separados de entidades; los controllers no conocen las entidades JPA.
- Validacion declarativa con Jakarta Validation (`@NotBlank`, etc.) y manejo global de errores en `exception.GlobalExceptionHandler`.
- `ExcelService` nunca crea un libro desde cero: siempre parte de la plantilla y solo completa celdas, preservando estilos/formulas.
- Si el analisis falla en cualquier punto, el proceso queda en estado `ERROR` (no se pierde la trazabilidad de que algo salio mal).
- `AuthService` valida credenciales directamente contra `UsuarioRepository` comparando la contrasena en texto plano (`.equals(...)`, sin BCrypt), sin pasar por el `AuthenticationManager` de Spring Security: con un solo tipo de credencial esa capa no aportaba nada y complicaba devolver el contrato `{success, message, token, username, role}` / `{success:false, message}` pedido. Spring Security si se usa para proteger el resto de rutas, incluida la autorizacion por rol (`security.SecurityConfig` + `auth.JwtAuthenticationFilter` + `security.JwtAccessDeniedHandler`).
