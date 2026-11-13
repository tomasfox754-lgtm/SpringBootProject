// Aqui centralizamos las peticiones al backend. No leemos roles del JWT.
let sesion = null;
try {
  sesion = JSON.parse(sessionStorage.getItem("logitrackSesion"));
} catch (error) {
  sessionStorage.removeItem("logitrackSesion");
}

async function peticion(ruta, metodo = "GET", datos = null) {
  const opciones = { method: metodo, headers: {} };
  if (sesion && sesion.token) {
    opciones.headers.Authorization = "Bearer " + sesion.token;
  }
  if (datos !== null) {
    opciones.headers["Content-Type"] = "application/json";
    opciones.body = JSON.stringify(datos);
  }

  let respuesta;
  try {
    respuesta = await fetch(API_URL + ruta, opciones);
  } catch (error) {
    throw new Error("No se pudo conectar. Comprueba que el backend este ejecutandose en IntelliJ y que aplicaste la configuracion de conexion indicada en el README.");
  }

  const texto = await respuesta.text();
  let cuerpo = null;
  if (texto) {
    try { cuerpo = JSON.parse(texto); }
    catch (error) { throw new Error("El servidor devolvio una respuesta inesperada."); }
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && sesion) {
      cerrarSesion();
      throw new Error("Tu sesion termino. Inicia sesion nuevamente.");
    }
    if (respuesta.status === 403) {
      throw new Error("Tu usuario no tiene permisos para realizar esta accion.");
    }
    let mensaje = cuerpo && cuerpo.mensaje ? cuerpo.mensaje : "No fue posible completar la solicitud.";
    if (cuerpo && cuerpo.errores) {
      const errores = Object.values(cuerpo.errores).join(". ");
      if (errores) mensaje += " " + errores;
    }
    throw new Error(mensaje);
  }
  return cuerpo;
}

function guardarSesion(datos) {
  // El rol es informacion de la respuesta del login, no del contenido del token.
  sesion = { token: datos.token, username: datos.username, rol: datos.rol };
  sessionStorage.setItem("logitrackSesion", JSON.stringify(sesion));
}

function borrarSesion() {
  sesion = null;
  sessionStorage.removeItem("logitrackSesion");
}
