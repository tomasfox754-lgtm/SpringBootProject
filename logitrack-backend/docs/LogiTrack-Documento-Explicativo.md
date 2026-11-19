# LogiTrack — Documento explicativo

**Sistema de gestion y auditoria de bodegas**  
**Tecnologías:** Java 17, Spring Boot 3.5.7, Spring Data JPA, Spring Security, JWT y MySQL.  
**Frontend:** HTML, CSS y JavaScript.

## 1. Descripción del proyecto

LogiTrack permite administrar bodegas y productos de tecnología, registrar entradas, salidas y transferencias de inventario, y consultar el historial de cambios realizados por cada usuario.

El backend se ejecuta en IntelliJ IDEA con Tomcat embebido, normalmente en `http://localhost:8080`. El frontend se edita en Visual Studio Code y se abre con Live Server, normalmente en `http://127.0.0.1:5500`. MySQL conserva los datos en la base `logitrack`.

Las cuentas configuradas para la entrega son `admin`, `empleado1`, `empleado2` y `empleado3`. El programa no limita tecnicamente la cantidad de cuentas a cuatro; esa es la carga de usuarios definida para el proyecto.

## 2. Diagrama de clases

El siguiente diagrama esta escrito en Mermaid. GitHub puede mostrarlo como un diagrama al abrir este Markdown. Si el visor muestra únicamente el código, debe utilizarse un visor compatible con Mermaid o una extensión de vista previa para VS Code.

```mermaid
classDiagram
    class EntidadAuditable {
        <<abstract>>
        +Long id
        +String valoresAnteriores
        +valoresAuditoria() Map
    }

    class Bodega {
        +String nombre
        +String ubicacion
        +Long capacidad
        +String encargado
    }

    class Producto {
        +String nombre
        +String categoria
        +Long stock
        +BigDecimal precio
    }

    class Usuario {
        +Long id
        +String username
        +String password
        +Rol rol
    }

    class Inventario {
        +Bodega bodega
        +Producto producto
        +Long cantidad
    }

    class Movimiento {
        +LocalDateTime fecha
        +TipoMovimiento tipo
        +Usuario usuario
        +Bodega origen
        +Bodega destino
        +List~DetalleMovimiento~ detalles
    }

    class DetalleMovimiento {
        +Long id
        +Movimiento movimiento
        +Producto producto
        +Integer cantidad
    }

    class Auditoria {
        +Long id
        +TipoOperacion operacion
        +LocalDateTime fecha
        +String usuario
        +String entidad
        +Long entidadId
        +String valoresAnteriores
        +String valoresNuevos
    }

    class AuditoriaListener {
        +recordar(entidad) void
        +insert(entidad) void
        +update(entidad) void
        +delete(entidad) void
    }

    class Rol {
        <<enumeration>>
        ADMIN
        EMPLEADO
    }

    class TipoMovimiento {
        <<enumeration>>
        ENTRADA
        SALIDA
        TRANSFERENCIA
    }

    class TipoOperacion {
        <<enumeration>>
        INSERT
        UPDATE
        DELETE
    }

    EntidadAuditable <|-- Bodega
    EntidadAuditable <|-- Producto
    EntidadAuditable <|-- Inventario
    EntidadAuditable <|-- Movimiento

    Inventario "0..*" --> "1" Bodega : bodega
    Inventario "0..*" --> "1" Producto : producto
    Movimiento "0..*" --> "1" Usuario : responsable
    Movimiento "0..*" --> "0..1" Bodega : origen
    Movimiento "0..*" --> "0..1" Bodega : destino
    Movimiento "1" *-- "1..*" DetalleMovimiento : contiene
    DetalleMovimiento "0..*" --> "1" Producto : producto

    Usuario --> Rol : tiene
    Movimiento --> TipoMovimiento : tipo
    Auditoria --> TipoOperacion : operacion
    EntidadAuditable ..> AuditoriaListener : callbacks JPA
    AuditoriaListener ..> Auditoria : registra cambios
```

### 2.1. Función de las clases

| Clase | Responsabilidad |
|---|---|
| `Bodega` | Guarda nombre, ubicación, capacidad y encargado. |
| `Producto` | Guarda nombre, categoría, precio y stock global. |
| `Usuario` | Guarda las credenciales cifradas y el rol de cada cuenta. |
| `Inventario` | Guarda la cantidad de un producto en una bodega concreta. |
| `Movimiento` | Registra fecha, tipo, usuario responsable, bodegas y detalles. |
| `DetalleMovimiento` | Indica qué producto y cuántas unidades incluye un movimiento. |
| `Auditoria` | Conserva operación, fecha, usuario, entidad y valores anteriores/nuevos. |
| `EntidadAuditable` | Clase padre que aporta el ID y el soporte para recordar el estado anterior. |
| `AuditoriaListener` | Registra automáticamente los cambios de las entidades auditadas. |

### 2.2. Relaciones y decisiones del modelo

- Una bodega puede almacenar varios productos y un producto puede estar en varias bodegas. `Inventario` representa esa relación y guarda la cantidad. La combinación `bodega_id` y `producto_id` es única.
- Un usuario puede registrar varios movimientos. Cada movimiento tiene un usuario responsable, obtenido de la autenticación del backend.
- Un movimiento contiene uno o más detalles. Cada detalle corresponde a un producto y una cantidad positiva.
- En una entrada se utiliza solamente la bodega destino; en una salida, solamente la bodega origen; en una transferencia, ambas y deben ser diferentes.
- `Bodega`, `Producto`, `Inventario` y `Movimiento` heredan el ID de `EntidadAuditable`. La clase padre usa `@MappedSuperclass` y no crea una tabla propia.
- `valoresAnteriores` en la clase padre es `@Transient`: solo recuerda el estado en memoria. En `Auditoria`, los campos anterior y nuevo sí se guardan en la base de datos.
- `Bodega.encargado` es un `String`. Guarda el nombre del encargado, sin una clave foránea hacia `Usuario`, según la decisión tomada para esta versión.
- `Auditoria.usuario` también es texto: conserva el nombre del responsable como dato histórico, sin una relación JPA con `Usuario`.

El esquema tiene siete tablas: `usuario`, `bodega`, `producto`, `inventario`, `movimiento`, `detalle_movimiento` y `auditoria`.

## 3. Descripción de la arquitectura

El sistema utiliza una arquitectura por capas. Cada capa tiene una responsabilidad definida para que el código sea más sencillo de leer y mantener.

```mermaid
flowchart TD
    A[Frontend HTML, CSS y JavaScript]
    B[Spring Security y JwtFilter]
    C[Controllers REST y validacion de DTO]
    D[Servicios: interfaces e implementaciones]
    E[Mappers: entidades y DTO]
    F[Repositories: Spring Data JPA]
    G[(MySQL: logitrack)]
    H[AuditoriaListener]
    I[GlobalExceptionHandler]

    A -->|HTTP y JSON| B
    B --> C
    C --> D
    D <--> E
    D --> F
    F --> G
    F -. callbacks de entidades .-> H
    H -->|JdbcTemplate, misma transaccion| G
    C -. errores .-> I
    D -. errores .-> I
    I -->|Respuesta JSON de error| A
```

El esquema representa responsabilidades. En la ejecución, JPA/Hibernate dispara el listener al gestionar las entidades; no existe una llamada manual al listener desde cada repository. Los errores de autenticación del filtro los responde `SeguridadErrorHandler`.

### 3.1. Organización del backend

```text
src/main/java/com/logitrack/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── service/
│   └── impl/
├── repository/
├── model/
├── audit/
├── config/
├── security/
└── exception/
```

| Paquete | Qué contiene | Clase representativa |
|---|---|---|
| `controller` | Endpoints HTTP y validación de las solicitudes. | `ProductoController` |
| `dto/request` | Campos permitidos en las solicitudes y sus validaciones. | `ProductoDtoRequest` |
| `dto/response` | Datos que devuelve la API, sin exponer las contraseñas. | `ProductoDtoResponse` |
| `mapper` | Conversión manual entre entidades y DTO. | `ProductoMapper` |
| `service` | Interfaces con las operaciones disponibles. | `ProductoService` |
| `service/impl` | Reglas del negocio y límites de transacción. | `MovimientoServiceImpl` |
| `repository` | Consultas y acceso a las entidades con Spring Data JPA. | `ProductoRepository` |
| `model` | Entidades de la base de datos y enumeraciones. | `Producto`, `TipoMovimiento` |
| `audit` | Auditoría automática mediante callbacks de JPA. | `AuditoriaListener` |
| `config` | Configuración de seguridad, CORS, Swagger y administrador inicial. | `SecurityConfig` |
| `security` | Generación del token, validación y carga de usuarios. | `JwtService`, `JwtFilter` |
| `exception` | Excepciones y respuestas JSON para errores. | `GlobalExceptionHandler` |

Los DTO utilizan `record`; las entidades y servicios utilizan Lombok. Los mappers son manuales y los servicios se separan en interfaz e implementación, manteniendo el estilo de programación del proyecto de referencia.

### 3.2. Recorrido de una entrada de inventario

1. El empleado llena el formulario del frontend o utiliza Swagger.
2. La solicitud llega con `Authorization: Bearer ...`.
3. `JwtFilter` verifica el token y carga al usuario y su rol actual desde MySQL.
4. Spring Security comprueba que el rol pueda registrar movimientos.
5. `MovimientoController` recibe y valida `MovimientoDtoRequest` con `@Valid`.
6. `MovimientoServiceImpl` verifica bodegas, productos, cantidades y capacidad disponible.
7. El servicio actualiza `Inventario` y el stock global de `Producto`, y guarda `Movimiento` con sus detalles.
8. Los callbacks de JPA generan las auditorías correspondientes.
9. La transacción se confirma y el controller devuelve el DTO del movimiento con HTTP `201`.

### 3.3. Reglas del inventario

`Producto.stock` representa el total entre todas las bodegas. `Inventario.cantidad` representa las existencias de ese producto en una bodega específica.

Por ejemplo, un teclado puede tener 15 unidades en Bogotá y 5 en Bucaramanga. Su stock global es 20. Una transferencia modifica la distribución, pero conserva ese total.

| Movimiento | Efecto en la bodega | Efecto en el stock global |
|---|---|---|
| ENTRADA | Aumenta las existencias en el destino. | Aumenta. |
| SALIDA | Disminuye las existencias en el origen. | Disminuye. |
| TRANSFERENCIA | Disminuye el origen y aumenta el destino. | Se conserva. |

Las cantidades deben ser positivas. Se rechazan el stock insuficiente, los productos repetidos en una misma solicitud y la capacidad excedida. La capacidad de una bodega se mide como unidades totales almacenadas.

Los productos nuevos comienzan con stock cero desde la API. Su stock se modifica mediante movimientos, no mediante el formulario de edición del producto. Los movimientos históricos no se editan ni eliminan. El backend también impide eliminar bodegas o productos que tengan inventario o movimientos asociados.

El servicio utiliza `@Transactional`: una operación se confirma completa o se revierte. Además, bloquea las bodegas y productos con bloqueo pesimista y ordena los bloqueos por ID para reducir conflictos entre operaciones concurrentes.

### 3.4. Auditoría automática

Las entidades auditadas usan el listener heredado de `EntidadAuditable`:

| Callback JPA | Función |
|---|---|
| `@PostLoad` | Recuerda el estado consultado en una propiedad temporal. |
| `@PostPersist` | Guarda una auditoría INSERT con los valores nuevos. |
| `@PostUpdate` | Guarda una auditoría UPDATE con valores anteriores y nuevos. |
| `@PreRemove` | Guarda una auditoría DELETE con los valores anteriores. |

`AuditoriaListener` obtiene al responsable desde `SecurityContextHolder`, convierte los valores a JSON y registra la auditoría usando `JdbcTemplate`. Esta inserción comparte la transacción de negocio: si falla la operación o falla la auditoría, se revierte la transacción.

La auditoría cubre `Bodega`, `Producto`, `Inventario` y `Movimiento`. El JSON del movimiento contiene sus productos y cantidades. `Usuario` no hereda el listener. Las instrucciones SQL ejecutadas directamente en la terminal no disparan estos callbacks; las auditorías incluidas en una carga SQL se insertan expresamente como parte de esa carga.

### 3.5. Frontend, CORS y documentación

El frontend utiliza `fetch` desde `js/api.js`. La dirección del backend se configura en `js/config.js`:

```javascript
const API_URL = "http://localhost:8080";
```

CORS permite que Live Server se comunique con Spring Boot. La configuración preparada acepta `http://localhost:5500`, `http://127.0.0.1:5500` y sus equivalentes en el puerto 5501. Los puertos 5500/5501 corresponden al frontend; el 8080 corresponde al backend.

CORS no verifica contraseñas ni asigna roles. La autenticación y los permisos se aplican con Spring Security y JWT.

Swagger permite documentar y probar la API en:

```text
http://localhost:8080/swagger-ui.html
```

La especificación OpenAPI se obtiene en `/v3/api-docs`. Las validaciones y reglas de negocio producen errores JSON mediante el manejo global de excepciones.

## 4. Ejemplo de token JWT y uso

### 4.1. Autenticación y autorización

La **autenticación** identifica al usuario. La **autorización** decide qué operaciones tiene permitidas.

En LogiTrack, el JWT identifica al usuario; los roles se guardan en `usuario.rol` y se consultan en MySQL en cada petición protegida. El JWT generado por el proyecto **no contiene rol, contraseña ni permisos**.

Al iniciar sesión, `AuthenticationManager` utiliza `UsuarioDetailsService` para obtener el usuario y `BCryptPasswordEncoder` para comparar la contraseña recibida con el hash almacenado. Si es correcta, `JwtService` genera el token firmado. Con la clave configurada para la práctica, se utiliza HS256.

### 4.2. Contenido de un JWT

El formato tiene tres partes:

```text
cabecera.contenido.firma
```

Cabecera:

```json
{"alg":"HS256"}
```

Contenido ilustrativo:

```json
{
  "sub": "empleado1",
  "iss": "logitrack",
  "iat": 1795096800,
  "exp": 1795100400
}
```

| Campo | Significado |
|---|---|
| `sub` | Nombre del usuario identificado. |
| `iss` | Emisor del token: `logitrack`. |
| `iat` | Fecha de emisión, en segundos Unix. |
| `exp` | Fecha de vencimiento, en segundos Unix. |

En este contenido, la emisión corresponde al 19 de noviembre de 2026 a las 09:00 y el vencimiento a las 10:00, hora de Colombia. La diferencia es de 3600 segundos: una hora. La configuración del proyecto es `jwt.expiration-ms=3600000`.

Token completo para estudiar su formato:

```text
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJlbXBsZWFkbzEiLCJpc3MiOiJsb2dpdHJhY2siLCJpYXQiOjE3OTUwOTY4MDAsImV4cCI6MTc5NTEwMDQwMH0.sMYOov7XDIWR5ha63ibVGNUIKUzUDs7FsGkFBCgRhqs
```

**Este token está firmado con una clave exclusiva de documentación, distinta a la del backend. No sirve para iniciar una sesión real.** Para hacer peticiones válidas debe obtenerse un token nuevo en `/auth/login`, como se explica a continuación.

El JWT está firmado, no cifrado. Su contenido puede leerse; la firma permite detectar cambios y comprobar que fue emitido con la clave correspondiente. Por eso no se incluyen contraseñas dentro del token.

### 4.3. Obtener un token válido

Con Spring Boot ejecutándose y el administrador configurado para la práctica:

```bash
curl -X POST 'http://localhost:8080/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin12345!"}'
```

La respuesta tiene esta estructura:

```json
{
  "token": "TOKEN_GENERADO_POR_EL_BACKEND",
  "tipo": "Bearer",
  "username": "admin",
  "rol": "ADMIN"
}
```

`rol` es un campo de la respuesta de login, separado del JWT. El frontend lo utiliza para mostrar u ocultar opciones; el backend siempre decide los permisos consultando la base de datos.

### 4.4. Enviar el token a un endpoint protegido

Copia el valor real de `token`, sin comillas, y reemplaza `PEGA_AQUI_EL_TOKEN_DEL_LOGIN`:

```bash
curl -X GET 'http://localhost:8080/bodegas' \
  -H 'Authorization: Bearer PEGA_AQUI_EL_TOKEN_DEL_LOGIN'
```

Para registrar una entrada, utiliza IDs que existan en tu base:

```bash
curl -X POST 'http://localhost:8080/movimientos' \
  -H 'Authorization: Bearer PEGA_AQUI_EL_TOKEN_DEL_LOGIN' \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo":"ENTRADA",
    "destinoId":1,
    "detalles":[{"productoId":1,"cantidad":10}]
  }'
```

En el frontend, `api.js` agrega el mismo encabezado automáticamente. El token se conserva en `sessionStorage` durante la sesión de esa pestaña; la contraseña no se guarda allí.

### 4.5. Usarlo en Swagger

1. Abre `/swagger-ui.html`.
2. En `POST /auth/login`, pulsa **Try it out**, envía las credenciales y ejecuta la petición.
3. Copia el valor de `token` de la respuesta HTTP 200.
4. En la parte superior derecha, pulsa **Authorize**.
5. Pega **solo el token**, sin escribir `Bearer` ni incluir comillas.
6. Pulsa **Authorize** y después **Close**.
7. Ejecuta un endpoint protegido, como `GET /bodegas`.

Si cambias de cuenta, utiliza **Logout** en el diálogo de autorización antes de pegar el token nuevo.

### 4.6. Verificación en cada petición

```mermaid
sequenceDiagram
    participant F as Frontend o Swagger
    participant J as JwtFilter y JwtService
    participant U as UsuarioDetailsService
    participant BD as MySQL
    participant S as Spring Security
    participant C as Controller

    F->>J: Peticion con Authorization Bearer
    J->>J: Verificar firma, emisor y vencimiento
    J->>U: Cargar usuario a partir de sub
    U->>BD: Consultar usuario y rol actual
    BD-->>U: Usuario con ADMIN o EMPLEADO
    U-->>J: UserDetails con permisos
    J->>S: Establecer autenticacion
    S->>S: Comprobar permiso de la ruta
    S->>C: Continuar si esta autorizado
    C-->>F: Respuesta HTTP y JSON
```

`JwtService` valida el token y obtiene `sub`. `UsuarioDetailsService` carga al usuario desde la base de datos y convierte su rol a una autoridad de Spring Security. `JwtFilter` coloca esa autenticación en `SecurityContextHolder`, y las reglas de `SecurityConfig` autorizan o rechazan la petición.

Como el rol se consulta en MySQL, un cambio de rol se aplica en la siguiente petición, incluso cuando el usuario conserva el mismo JWT. Agregar manualmente un campo `rol` a un token no asigna permisos: la modificación invalida la firma, y el backend no usa ese campo como fuente de autoridad.

### 4.7. Permisos y respuestas

| Acción | ADMIN | EMPLEADO |
|---|---|---|
| Consultar bodegas, productos e inventarios | Sí | Sí |
| Crear, actualizar y eliminar bodegas/productos | Sí | No |
| Registrar y consultar movimientos | Sí | Sí |
| Consultar stock bajo y reportes | Sí | Sí |
| Consultar auditorías | Sí | No |

En la versión entregada, `/auth/login` y `/auth/register` son públicos. Login exige credenciales válidas. El registro crea únicamente EMPLEADO y no acepta seleccionar ADMIN. Las cuatro cuentas de la carga inicial no restringen por sí solas este endpoint de registro.

| Código | Situación |
|---|---|
| `200` | Consulta o login correctos. |
| `201` | Creación o movimiento registrado. |
| `204` | Eliminación correcta, sin contenido. |
| `400` | Validación, formato o regla de negocio incumplida. |
| `401` | Credenciales incorrectas, token ausente, inválido o vencido. |
| `403` | Usuario autenticado sin permiso para la operación. |
| `404` | Recurso inexistente. |
| `500` | Error interno inesperado. |

Cerrar sesión elimina el token del navegador. El backend no implementa una lista de revocación: un token copiado puede seguir siendo válido hasta su vencimiento si se mantiene su firma válida y el usuario continúa existiendo. La expiración obliga a volver a iniciar sesión para obtener uno nuevo.

## 5. Explicación breve para sustentar

> LogiTrack utiliza Spring Boot con una arquitectura por capas. Los controllers reciben las peticiones, los DTO validan y delimitan los datos, los servicios aplican las reglas y los repositories acceden a MySQL. Inventario guarda las existencias por bodega y Producto mantiene el total. Los movimientos se ejecutan dentro de una transacción y sus cambios se auditan mediante listeners de JPA. El JWT identifica al usuario, mientras que el backend consulta su rol en la base de datos y Spring Security controla los permisos.
