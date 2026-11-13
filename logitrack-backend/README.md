# LogiTrack — Backend

Proyecto Spring Boot 3.5.7, Java 17+, Maven, MySQL, JWT y auditoría automática.
Incluye código fuente, pruebas, scripts SQL y Swagger. No incluye frontend.

## Abrir en IntelliJ

1. Descomprime el ZIP y abre `pom.xml` como proyecto Maven.
2. Selecciona JDK 17 o 21 y permite descargar las dependencias.
3. Activa el procesamiento de anotaciones para Lombok en Settings → Build, Execution, Deployment → Compiler → Annotation Processors.
4. Para probar sin instalar MySQL, ejecuta `LogitrackApplication` con `--spring.profiles.active=demo` en Program arguments.
5. Abre http://localhost:8080/swagger-ui.html.
6. En POST /auth/login usa `admin` / `Admin12345!` (solo perfil demo).
7. Copia el token recibido, pulsa Authorize y pega solo el token.

El perfil demo usa H2 en memoria y pierde los datos al detenerse.

## MySQL

Crea una base `logitrack` en MySQL 8.0.16+ y configura las variables de entorno en IntelliJ:

```
DB_URL=jdbc:mysql://localhost:3306/logitrack?serverTimezone=America/Bogota
DB_USER=tu_usuario
DB_PASSWORD=tu_clave
JWT_SECRET=tu-clave-aleatoria-de-al-menos-32-bytes
ADMIN_USERNAME=admin
ADMIN_PASSWORD=tu-clave-de-8-a-60-caracteres
```

No actives demo al utilizar MySQL. `schema.sql` y `data.sql` se ejecutan automáticamente; el administrador se crea si no existe.

## Terminal

Linux/macOS:

```bash
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
.\mvnw.cmd test
```

## Rutas principales

- POST /auth/register y /auth/login: públicas; el registro crea EMPLEADO.
- GET /bodegas y /productos: ambos roles; POST, PUT y DELETE: ADMIN.
- GET /bodegas/{id}/inventario: existencias por producto.
- POST y GET /movimientos: ADMIN o EMPLEADO.
- GET /productos/stock-bajo: stock global menor a diez.
- GET /movimientos?desde=2026-11-01T00:00:00&hasta=2026-11-30T23:59:59: fechas inclusivas.
- GET /auditorias?usuario=admin&operacion=UPDATE: ADMIN.
- GET /reportes/resumen: stock por bodega y productos más movidos.

El stock comienza en cero y se cambia mediante movimientos. Una entrada requiere destinoId; una salida origenId; una transferencia ambos IDs diferentes. El usuario responsable se obtiene del JWT. No se editan ni eliminan movimientos históricos.

Entrada de ejemplo (consulta primero los IDs):

```json
{"tipo":"ENTRADA","destinoId":1,"detalles":[{"productoId":1,"cantidad":10}]}
```

La auditoría mediante listeners JPA guarda usuario, entidad y valores anteriores/nuevos. Las operaciones y sus auditorías se revierten juntas si ocurre un fallo.

Las 15 pruebas de integración del proyecto se verificaron tanto con H2 como con MySQL 8.4.

## Roles administrados solo en el backend

El JWT generado no contiene rol ni permisos: solo identifica al usuario mediante `sub` y contiene emisor y fechas de emisión/vencimiento. En cada solicitud, `UsuarioDetailsService` consulta el rol actual en la tabla `usuario`, y Spring Security aplica los permisos configurados en `SecurityConfig`. Un cambio de rol en la base de datos se aplica a la siguiente petición, incluso con el mismo token.

El registro público no acepta roles y crea EMPLEADO; el administrador inicial se configura en el backend. Para cambiar un rol desde la administración de la base de datos:

```sql
UPDATE usuario SET rol = 'ADMIN' WHERE username = 'empleado1';
-- Para volver a empleado:
UPDATE usuario SET rol = 'EMPLEADO' WHERE username = 'empleado1';
```

El campo `rol` de la respuesta de login es información del usuario, separado del JWT; no se utiliza para autorizar peticiones. La autorización se decide siempre en el backend.
