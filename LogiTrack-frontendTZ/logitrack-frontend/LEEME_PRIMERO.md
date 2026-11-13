# LogiTrack — Frontend para Visual Studio Code

Este proyecto usa solo HTML, CSS y JavaScript. No necesitas Node, npm, React ni Angular.
Los datos se consultan y guardan en tu backend Spring Boot. La base de datos sigue siendo MySQL.

## 1. Configurar la conexion en el backend (una sola vez)

El frontend abre en el puerto 5500 y Spring Boot en el 8080. El navegador requiere que el backend autorice esa conexion mediante CORS.

1. Deten el backend en IntelliJ con el boton rojo.
2. Dentro del ZIP del frontend, abre `integracion-backend/SecurityConfig.java`.
3. Copia TODO su contenido.
4. En IntelliJ abre tu archivo:

```
src/main/java/com/logitrack/config/SecurityConfig.java
```

5. Reemplaza TODO el contenido de ese archivo por el que copiaste y guarda.
6. No cambies `application.properties`: conserva tu conexion a MySQL y `spring.profiles.active=mysql`.
7. Ejecuta `LogitrackApplication` y espera a que diga `Started LogitrackApplication` y perfil `mysql`.

El archivo conserva JWT y las reglas de ADMIN/EMPLEADO. Agrega permisos CORS para `localhost` y `127.0.0.1` en puertos 5500 y 5501. No habilita acceso desde cualquier pagina ni convierte los endpoints en publicos.

## 2. Abrir el frontend en Visual Studio Code

1. Descomprime `LogiTrack-frontend.zip` en una carpeta nueva.
2. En VS Code selecciona **File → Open Folder** y abre la carpeta `logitrack-frontend` (la que contiene `index.html`).
3. Pulsa **Ctrl + Shift + X** para abrir las extensiones.
4. Busca **Live Server**, del autor **Ritwick Dey**, e instalala.
5. Abre `index.html`.
6. Haz clic derecho dentro del archivo → **Open with Live Server**. Tambien puedes usar **Go Live**, abajo a la derecha.
7. El navegador debe abrir `http://127.0.0.1:5500`.

La carpeta `.vscode` fija ese puerto. Si esta ocupado, cierra otra ventana de Live Server o utiliza 5501. Para otro puerto debes agregar su origen en el CORS del backend.

No abras `index.html` con doble clic desde el explorador: debe abrirse mediante Live Server.

## 3. Iniciar sesion

Usa las credenciales de tu backend MySQL:

- Usuario: `admin`
- Contrasena: `Admin12345!`

Si ya cambiaste esas credenciales, utiliza las que configuraste. No se rellenan ni se guardan contrasenas en el codigo del frontend.

Despues de iniciar sesion, veras el resumen de tus datos reales. Los movimientos que registraste en Swagger tambien aparecen aqui si estas conectado a la misma base de datos.

## 4. Que puedes probar

- **Resumen:** bodegas, productos, stock total, productos con stock bajo, ultimos movimientos.
- **Bodegas:** consultar, buscar, crear, editar, eliminar y ver inventario. Crear/editar/eliminar requieren ADMIN.
- **Productos:** consultar, buscar, filtrar stock bajo y CRUD de ADMIN. El stock se cambia desde movimientos.
- **Movimientos:** entrada, salida o transferencia, con uno o varios productos. Filtro por rango de fechas.
- **Reportes:** existencias por bodega y productos mas movidos.
- **Auditorias:** filtros por usuario/operacion y detalle anterior/nuevo. Solo ADMIN.
- **Crear cuenta:** registro de empleados desde la pantalla de acceso. No permite elegir un rol.

El encargado de la bodega sigue siendo texto, como acordamos. No se crean ni cambian tus datos por abrir la pagina. Solo se modifican al guardar una accion en un formulario.

## 5. Como se organiza el codigo

```
logitrack-frontend/
  index.html                       Pantallas, tablas y formularios
  css/styles.css                   Colores neutros y diseno adaptable
  js/config.js                     Direccion del backend
  js/api.js                        Peticiones fetch y sesion
  js/app.js                        Eventos y funciones de las pantallas
  integracion-backend/
    SecurityConfig.java            Archivo para reemplazar en IntelliJ
  .vscode/
    settings.json                  Puerto de Live Server
    extensions.json                Extension recomendada
```

Empieza leyendo `index.html`. Cada seccion tiene un ID que se utiliza en `app.js`. Los comentarios separan login, bodegas, productos, movimientos y reportes.

En `js/api.js`, `peticion` usa `fetch`, agrega `Authorization: Bearer ...`, interpreta el JSON y muestra los errores que envia el backend. Para enviar datos usa POST/PUT; para eliminar usa DELETE.

Los botones usan `data-accion` y un listener al final de `app.js`. Las funciones se llaman de forma directa: `abrirBodega`, `mostrarProductos`, `abrirMovimiento`, etc. No hay herramientas de compilacion.

## 6. Roles y token

El frontend NO decodifica roles desde el JWT y no puede asignarlos.
El backend consulta el rol en MySQL y Spring Security decide si autoriza cada accion.
La respuesta de login incluye el rol del usuario por separado del token. Solo se usa para ocultar o mostrar opciones en la pantalla; ocultar botones no reemplaza la seguridad del servidor.

La sesion se conserva en `sessionStorage` durante la pestaña actual. Se borra al cerrar sesion. Si el backend responde 401 por token vencido, la aplicacion vuelve al login. Las contrasenas no se guardan en el almacenamiento del navegador.

## 7. Si no conecta

**Mensaje de conexion:** comprueba que Spring Boot esta iniciado, que Swagger abre y que reemplazaste `SecurityConfig.java` y reiniciaste el backend. Verifica que la pagina usa `http://127.0.0.1:5500`, `http://localhost:5500` o puerto 5501.

**Puerto del backend distinto:** cambia `API_URL` en `js/config.js`. Usa el mismo puerto en el que abre Swagger. Puedes utilizar `http://127.0.0.1:8080` si tu sistema no resuelve localhost.

**403:** la cuenta no tiene permiso para esa accion. Debes usar ADMIN para crear catalogos y consultar auditorias.

**Stock insuficiente o capacidad:** es una validacion del backend; revisa existencias o capacidad de la bodega. El formulario muestra el mensaje y conserva lo escrito para corregirlo.

**No aparecen los datos de ayer:** confirma que el backend arranco con perfil `mysql`, no `demo`.

**Un usuario no puede borrar un producto/bodega:** los registros con inventario o movimientos asociados conservan su historial; el backend impide borrarlos.

No es necesario volver a crear la base ni borrar tus registros para integrar este frontend.
