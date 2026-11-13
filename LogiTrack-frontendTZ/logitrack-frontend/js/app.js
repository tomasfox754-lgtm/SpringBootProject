// Estado de la pantalla. Los datos reales siempre vienen del backend.
let vistaActual = "inicio";
let bodegas = [];
let productos = [];
let movimientos = [];
let auditorias = [];
let eliminacionPendiente = null;
let temporizadorNotificacion;

function elemento(id) { return document.getElementById(id); }
function esAdmin() { return sesion && sesion.rol === "ADMIN"; }

// Evita interpretar los nombres recibidos como codigo HTML.
function escapar(valor) {
  return String(valor == null ? "—" : valor).replace(/[&<>"']/g, function (caracter) {
    return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[caracter];
  });
}
function numero(valor) { return Number(valor).toLocaleString("es-CO"); }
function precio(valor) {
  return "$ " + Number(valor).toLocaleString("es-CO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function fecha(valor) {
  if (!valor) return "—";
  // El backend utiliza fecha/hora local de Colombia, sin offset.
  const partes = valor.split("T");
  const dia = partes[0].split("-").reverse().join("/");
  return dia + " · " + (partes[1] || "").slice(0, 8);
}
function celda(valor, clase = "") { return '<td class="' + clase + '">' + escapar(valor) + "</td>"; }
function accion(nombre, texto, id, clase = "") {
  return '<button type="button" class="' + clase + '" data-accion="' + nombre + '" data-id="' + Number(id) + '">' + texto + "</button>";
}
function insignia(tipo) {
  const nombres = { ENTRADA: "Entrada", SALIDA: "Salida", TRANSFERENCIA: "Transferencia", INSERT: "Creación", UPDATE: "Actualización", DELETE: "Eliminación" };
  const clases = { ENTRADA: "", SALIDA: "salida", TRANSFERENCIA: "transferencia", INSERT: "", UPDATE: "transferencia", DELETE: "eliminacion" };
  return '<span class="insignia ' + (clases[tipo] || "") + '">' + escapar(nombres[tipo] || tipo) + "</span>";
}
function tabla(id, encabezados, filas, vacio = "No hay registros para mostrar.") {
  if (!filas.length) {
    elemento(id).innerHTML = '<div class="vacio">' + escapar(vacio) + "</div>";
    return;
  }
  elemento(id).innerHTML = "<table><thead><tr>" + encabezados.map(e => "<th scope=\"col\">" + escapar(e) + "</th>").join("") + "</tr></thead><tbody>" + filas.join("") + "</tbody></table>";
}
function notificar(mensaje, error = false) {
  clearTimeout(temporizadorNotificacion);
  elemento("notificacion").textContent = mensaje;
  elemento("notificacion").classList.toggle("error", error);
  elemento("notificacion").hidden = false;
  temporizadorNotificacion = setTimeout(() => { elemento("notificacion").hidden = true; }, 6500);
}
function errorFormulario(formulario, mensaje) {
  const salida = formulario.querySelector(".error-formulario");
  if (salida) { salida.textContent = mensaje; salida.hidden = !mensaje; }
}
function abrirModal(id) {
  const modal = elemento(id);
  const salida = modal.querySelector(".error-formulario");
  if (salida) salida.hidden = true;
  modal.showModal();
}
async function tareaFormulario(evento, trabajo) {
  evento.preventDefault();
  const formulario = evento.currentTarget;
  const boton = formulario.querySelector('[type="submit"]');
  boton.disabled = true;
  errorFormulario(formulario, "");
  try { await trabajo(); }
  catch (error) { errorFormulario(formulario, error.message); }
  finally { boton.disabled = false; }
}

// LOGIN Y REGISTRO
function mostrarAplicacion() {
  elemento("pantalla-acceso").hidden = true;
  elemento("pantalla-app").hidden = false;
  elemento("nombre-usuario").textContent = sesion.username;
  elemento("rol-usuario").textContent = esAdmin() ? "Administrador" : "Empleado";
  elemento("avatar-usuario").textContent = sesion.username.charAt(0).toUpperCase();
  // Ocultar botones es una ayuda visual. Spring Security decide todos los permisos.
  document.querySelectorAll(".solo-admin").forEach(e => { e.hidden = !esAdmin(); });
}
function cerrarSesion() {
  borrarSesion();
  bodegas = []; productos = []; movimientos = []; auditorias = [];
  eliminacionPendiente = null;
  document.querySelectorAll("dialog[open]").forEach(d => d.close());
  document.querySelectorAll(".contenedor-tabla").forEach(t => { t.innerHTML = ""; });
  elemento("pantalla-app").hidden = true;
  elemento("pantalla-acceso").hidden = false;
  elemento("form-login").reset();
  elemento("form-registro").reset();
  elemento("form-registro").hidden = true;
  elemento("form-login").hidden = false;
  actualizarTextoAcceso(false);
}
function actualizarTextoAcceso(registro) {
  elemento("acceso-titulo").textContent = registro ? "Crea tu cuenta" : "Inicia sesión";
  elemento("acceso-etiqueta").textContent = registro ? "UN NUEVO COMIENZO" : "BIENVENIDO DE NUEVO";
  elemento("acceso-descripcion").textContent = registro ? "Regístrate como empleado para participar en la operación." : "Ingresa con tu cuenta para continuar.";
  elemento("cambio-pregunta").textContent = registro ? "¿Ya tienes una cuenta?" : "¿Necesitas una cuenta?";
  elemento("cambiar-acceso").textContent = registro ? "Iniciar sesión" : "Crear cuenta de empleado";
  elemento("error-acceso").hidden = true;
  elemento("exito-acceso").hidden = true;
}
elemento("cambiar-acceso").addEventListener("click", function () {
  const registro = elemento("form-registro").hidden;
  elemento("form-registro").hidden = !registro;
  elemento("form-login").hidden = registro;
  actualizarTextoAcceso(registro);
});
elemento("form-login").addEventListener("submit", async function (e) {
  e.preventDefault();
  const boton = e.submitter;
  boton.disabled = true;
  elemento("error-acceso").hidden = true;
  elemento("exito-acceso").hidden = true;
  try {
    const datos = await peticion("/auth/login", "POST", { username: elemento("login-usuario").value.trim(), password: elemento("login-clave").value });
    guardarSesion(datos);
    elemento("login-clave").value = "";
    mostrarAplicacion();
    await cambiarVista("inicio");
  } catch (error) {
    elemento("error-acceso").textContent = error.message;
    elemento("error-acceso").hidden = false;
  } finally { boton.disabled = false; }
});
elemento("form-registro").addEventListener("submit", async function (e) {
  e.preventDefault();
  const boton = e.submitter;
  boton.disabled = true;
  elemento("error-acceso").hidden = true;
  elemento("exito-acceso").hidden = true;
  try {
    const username = elemento("registro-usuario").value.trim();
    await peticion("/auth/register", "POST", { username: username, password: elemento("registro-clave").value });
    elemento("form-registro").reset();
    elemento("form-registro").hidden = true;
    elemento("form-login").hidden = false;
    actualizarTextoAcceso(false);
    elemento("login-usuario").value = username;
    elemento("exito-acceso").textContent = "Cuenta creada. Ya puedes iniciar sesión.";
    elemento("exito-acceso").hidden = false;
  } catch (error) {
    elemento("error-acceso").textContent = error.message;
    elemento("error-acceso").hidden = false;
  } finally { boton.disabled = false; }
});
elemento("cerrar-sesion").addEventListener("click", function () { cerrarSesion(); });

// NAVEGACION Y CARGA DE DATOS
async function cambiarVista(vista) {
  if (vista === "auditorias" && !esAdmin()) { notificar("Solo el administrador puede consultar auditorías.", true); return; }
  const titulos = { inicio: "Resumen general", bodegas: "Bodegas", productos: "Productos", movimientos: "Movimientos", reportes: "Reportes", auditorias: "Auditorías" };
  vistaActual = vista;
  document.querySelectorAll(".vista").forEach(s => { s.hidden = s.id !== "vista-" + vista; });
  document.querySelectorAll(".enlace-menu").forEach(b => { b.classList.toggle("activo", b.dataset.vista === vista); });
  elemento("titulo-vista").textContent = titulos[vista];
  elemento("estado-carga").hidden = false;
  try {
    if (vista === "inicio") await cargarInicio();
    if (vista === "bodegas") { bodegas = await peticion("/bodegas"); mostrarBodegas(); }
    if (vista === "productos") {
      const ruta = elemento("filtro-stock").value === "bajo" ? "/productos/stock-bajo" : "/productos";
      productos = await peticion(ruta); mostrarProductos();
    }
    if (vista === "movimientos") await cargarMovimientos();
    if (vista === "reportes") await cargarReportes();
    if (vista === "auditorias") await cargarAuditorias();
  } catch (error) { notificar(error.message, true); }
  finally { elemento("estado-carga").hidden = true; }
}
elemento("menu").addEventListener("click", function (e) {
  const boton = e.target.closest("[data-vista]");
  if (boton) cambiarVista(boton.dataset.vista);
});
elemento("actualizar").addEventListener("click", async function () {
  this.disabled = true;
  try { await cambiarVista(vistaActual); }
  finally { this.disabled = false; }
});
async function cargarCatalogos() {
  const datos = await Promise.all([peticion("/bodegas"), peticion("/productos")]);
  bodegas = datos[0]; productos = datos[1];
}
async function cargarInicio() {
  const datos = await Promise.all([peticion("/bodegas"), peticion("/productos"), peticion("/reportes/resumen"), peticion("/movimientos")]);
  bodegas = datos[0]; productos = datos[1]; movimientos = datos[3];
  const resumen = datos[2];
  elemento("indicador-bodegas").textContent = numero(bodegas.length);
  elemento("indicador-productos").textContent = numero(productos.length);
  elemento("indicador-stock").textContent = numero(resumen.stockPorBodega.reduce((total, b) => total + b.stockTotal, 0));
  elemento("indicador-bajo").textContent = numero(productos.filter(p => p.stock < 10).length);
  const maximo = Math.max(1, ...resumen.stockPorBodega.map(b => b.stockTotal));
  elemento("barras-bodegas").innerHTML = resumen.stockPorBodega.length ? resumen.stockPorBodega.map(function (b) {
    return '<div class="barra-item"><div class="barra-descripcion"><span>' + escapar(b.nombre) + '</span><span>' + numero(b.stockTotal) + ' unidades</span></div><div class="barra-fondo"><div class="barra-relleno" style="width:' + (b.stockTotal / maximo * 100) + '%"></div></div></div>';
  }).join("") : '<div class="vacio">Todavía no hay bodegas registradas.</div>';
  elemento("ranking-productos").innerHTML = resumen.productosMasMovidos.length ? resumen.productosMasMovidos.slice(0, 5).map(function (p, i) {
    return '<div class="ranking-fila"><span class="ranking-numero">0' + (i + 1) + '</span><strong>' + escapar(p.nombre) + '</strong><small>' + numero(p.cantidadMovida) + ' uds.</small></div>';
  }).join("") : '<div class="vacio">Registra tu primer movimiento para ver la actividad.</div>';
  mostrarMovimientos("tabla-recientes", movimientos.slice(0, 5));
}

// BODEGAS: consultar, crear, actualizar y eliminar.
function mostrarBodegas() {
  const busqueda = elemento("buscar-bodega").value.toLowerCase().trim();
  const lista = bodegas.filter(b => (b.nombre + " " + b.ubicacion + " " + b.encargado).toLowerCase().includes(busqueda));
  const filas = lista.map(function (b) {
    let acciones = accion("inventario", "Ver inventario", b.id);
    if (esAdmin()) acciones += accion("editar-bodega", "Editar", b.id) + accion("eliminar-bodega", "Eliminar", b.id, "eliminar");
    return "<tr>" + celda("#" + b.id) + celda(b.nombre, "nombre-celda") + celda(b.ubicacion) + celda(numero(b.capacidad)) + celda(b.encargado) + '<td><div class="acciones-tabla">' + acciones + "</div></td></tr>";
  });
  tabla("tabla-bodegas", ["ID", "Bodega", "Ubicación", "Capacidad", "Encargado", "Acciones"], filas);
  elemento("contador-bodegas").textContent = lista.length + " bodegas encontradas";
}
elemento("buscar-bodega").addEventListener("input", mostrarBodegas);
function abrirBodega(id) {
  const b = id ? bodegas.find(b => b.id === id) : null;
  elemento("form-bodega").reset();
  elemento("bodega-id").value = b ? b.id : "";
  elemento("titulo-bodega").textContent = b ? "Editar bodega" : "Nueva bodega";
  if (b) {
    elemento("bodega-nombre").value = b.nombre;
    elemento("bodega-ubicacion").value = b.ubicacion;
    elemento("bodega-capacidad").value = b.capacidad;
    elemento("bodega-encargado").value = b.encargado;
  }
  abrirModal("modal-bodega");
}
elemento("form-bodega").addEventListener("submit", function (e) {
  tareaFormulario(e, async function () {
    const id = elemento("bodega-id").value;
    const datos = { nombre: elemento("bodega-nombre").value.trim(), ubicacion: elemento("bodega-ubicacion").value.trim(), capacidad: Number(elemento("bodega-capacidad").value), encargado: elemento("bodega-encargado").value.trim() };
    await peticion(id ? "/bodegas/" + id : "/bodegas", id ? "PUT" : "POST", datos);
    elemento("modal-bodega").close();
    notificar(id ? "Bodega actualizada." : "Bodega creada correctamente.");
    await cambiarVista("bodegas");
  });
});
async function verInventario(id) {
  const datos = await peticion("/bodegas/" + id + "/inventario");
  const bodega = bodegas.find(b => b.id === id);
  elemento("titulo-inventario").textContent = bodega ? bodega.nombre : "Inventario de bodega";
  tabla("tabla-inventario", ["ID producto", "Producto", "Cantidad"], datos.map(i => "<tr>" + celda(i.productoId) + celda(i.producto, "nombre-celda") + celda(numero(i.cantidad)) + "</tr>"), "Esta bodega todavía no tiene inventario.");
  abrirModal("modal-inventario");
}

// PRODUCTOS
function mostrarProductos() {
  const busqueda = elemento("buscar-producto").value.toLowerCase().trim();
  const soloBajo = elemento("filtro-stock").value === "bajo";
  const lista = productos.filter(p => (p.nombre + " " + p.categoria).toLowerCase().includes(busqueda) && (!soloBajo || p.stock < 10));
  const filas = lista.map(function (p) {
    let acciones = esAdmin() ? accion("editar-producto", "Editar", p.id) + accion("eliminar-producto", "Eliminar", p.id, "eliminar") : "—";
    return "<tr>" + celda("#" + p.id) + celda(p.nombre, "nombre-celda") + celda(p.categoria) + celda(precio(p.precio)) + celda(numero(p.stock)) + '<td><span class="insignia ' + (p.stock < 10 ? "bajo" : "") + '">' + (p.stock < 10 ? "Stock bajo" : "Disponible") + '</span></td><td><div class="acciones-tabla">' + acciones + "</div></td></tr>";
  });
  tabla("tabla-productos", ["ID", "Producto", "Categoría", "Precio", "Stock total", "Estado", "Acciones"], filas);
  elemento("contador-productos").textContent = lista.length + " productos encontrados";
}
elemento("buscar-producto").addEventListener("input", mostrarProductos);
elemento("filtro-stock").addEventListener("change", function () { cambiarVista("productos"); });
function abrirProducto(id) {
  const p = id ? productos.find(p => p.id === id) : null;
  elemento("form-producto").reset();
  elemento("producto-id").value = p ? p.id : "";
  elemento("titulo-producto").textContent = p ? "Editar producto" : "Nuevo producto";
  if (p) {
    elemento("producto-nombre").value = p.nombre;
    elemento("producto-categoria").value = p.categoria;
    elemento("producto-precio").value = p.precio;
  }
  abrirModal("modal-producto");
}
elemento("form-producto").addEventListener("submit", function (e) {
  tareaFormulario(e, async function () {
    const id = elemento("producto-id").value;
    const datos = { nombre: elemento("producto-nombre").value.trim(), categoria: elemento("producto-categoria").value.trim(), precio: Number(elemento("producto-precio").value) };
    await peticion(id ? "/productos/" + id : "/productos", id ? "PUT" : "POST", datos);
    elemento("modal-producto").close();
    notificar(id ? "Producto actualizado." : "Producto creado. Puedes registrar una entrada para agregar stock.");
    await cambiarVista("productos");
  });
});

// ELIMINACION CON CONFIRMACION
function confirmarEliminar(tipo, id) {
  const lista = tipo === "bodegas" ? bodegas : productos;
  const registro = lista.find(r => r.id === id);
  eliminacionPendiente = { tipo: tipo, id: id };
  elemento("texto-confirmar").textContent = '¿Deseas eliminar "' + registro.nombre + '"? Esta acción no se puede deshacer.';
  elemento("error-confirmar").hidden = true;
  abrirModal("modal-confirmar");
}
elemento("confirmar-eliminar").addEventListener("click", async function () {
  if (!eliminacionPendiente) return;
  this.disabled = true;
  elemento("error-confirmar").hidden = true;
  const pendiente = eliminacionPendiente;
  try {
    await peticion("/" + pendiente.tipo + "/" + pendiente.id, "DELETE");
    elemento("modal-confirmar").close(); eliminacionPendiente = null;
    notificar("Registro eliminado correctamente.");
    await cambiarVista(pendiente.tipo);
  } catch (error) {
    elemento("error-confirmar").textContent = error.message;
    elemento("error-confirmar").hidden = false;
  } finally { this.disabled = false; }
});

// MOVIMIENTOS
function nombreBodega(id) {
  if (id == null) return "—";
  const b = bodegas.find(b => b.id === id);
  return b ? b.nombre : "Bodega #" + id;
}
function mostrarMovimientos(idTabla, lista) {
  const filas = lista.map(function (m) {
    const detalles = m.detalles.map(d => d.producto + " × " + d.cantidad).join(", ");
    return "<tr>" + celda("#" + m.id) + celda(fecha(m.fecha)) + "<td>" + insignia(m.tipo) + "</td>" + celda(detalles) + celda(nombreBodega(m.origenId)) + celda(nombreBodega(m.destinoId)) + celda(m.usuario) + "</tr>";
  });
  tabla(idTabla, ["ID", "Fecha", "Tipo", "Productos", "Origen", "Destino", "Responsable"], filas, "Todavía no hay movimientos en este rango.");
}
async function cargarMovimientos() {
  await cargarCatalogos();
  const desde = elemento("fecha-desde").value;
  const hasta = elemento("fecha-hasta").value;
  let ruta = "/movimientos";
  if (desde && hasta) ruta += "?" + new URLSearchParams({ desde: desde, hasta: hasta });
  movimientos = await peticion(ruta);
  mostrarMovimientos("tabla-movimientos", movimientos);
  elemento("contador-movimientos").textContent = movimientos.length + " movimientos encontrados";
}
elemento("form-filtro-fechas").addEventListener("submit", async function (e) {
  e.preventDefault();
  if (elemento("fecha-desde").value > elemento("fecha-hasta").value) { notificar("La fecha inicial no puede ser posterior a la final.", true); return; }
  await cambiarVista("movimientos");
});
elemento("limpiar-fechas").addEventListener("click", function () {
  elemento("form-filtro-fechas").reset(); cambiarVista("movimientos");
});
function opcionesBodegas() {
  return '<option value="">Selecciona una bodega</option>' + bodegas.map(b => '<option value="' + b.id + '">' + escapar(b.nombre) + "</option>").join("");
}
async function abrirMovimiento() {
  await cargarCatalogos();
  if (!bodegas.length || !productos.length) { notificar("Necesitas al menos una bodega y un producto registrados.", true); return; }
  elemento("form-movimiento").reset();
  elemento("movimiento-origen").innerHTML = opcionesBodegas();
  elemento("movimiento-destino").innerHTML = opcionesBodegas();
  elemento("detalles-movimiento").innerHTML = "";
  agregarDetalle(); actualizarTipoMovimiento();
  abrirModal("modal-movimiento");
}
function actualizarTipoMovimiento() {
  const tipo = elemento("movimiento-tipo").value;
  const usaOrigen = tipo !== "ENTRADA";
  const usaDestino = tipo !== "SALIDA";
  elemento("campo-origen").hidden = !usaOrigen;
  elemento("campo-destino").hidden = !usaDestino;
  elemento("movimiento-origen").required = usaOrigen;
  elemento("movimiento-origen").disabled = !usaOrigen;
  elemento("movimiento-destino").required = usaDestino;
  elemento("movimiento-destino").disabled = !usaDestino;
  if (!usaOrigen) elemento("movimiento-origen").value = "";
  if (!usaDestino) elemento("movimiento-destino").value = "";
}
elemento("movimiento-tipo").addEventListener("change", actualizarTipoMovimiento);
function agregarDetalle() {
  if (elemento("detalles-movimiento").children.length >= 100) { notificar("Un movimiento admite hasta 100 productos.", true); return; }
  const fila = document.createElement("div"); fila.className = "fila-detalle";
  // Las opciones se construyen con nombres escapados, sin ejecutar HTML de los datos.
  fila.innerHTML = '<label>Producto<select class="detalle-producto" required><option value="">Selecciona un producto</option>' + productos.map(p => '<option value="' + p.id + '">' + escapar(p.nombre) + '</option>').join("") + '</select></label><label>Cantidad<input class="detalle-cantidad" type="number" min="1" max="1000000" step="1" value="1" required></label><button type="button" class="quitar-detalle" aria-label="Quitar producto">×</button>';
  elemento("detalles-movimiento").append(fila);
}
elemento("agregar-detalle").addEventListener("click", agregarDetalle);
elemento("detalles-movimiento").addEventListener("click", function (e) {
  const boton = e.target.closest(".quitar-detalle"); if (!boton) return;
  if (this.children.length === 1) { notificar("Incluye al menos un producto en el movimiento.", true); return; }
  boton.closest(".fila-detalle").remove();
});
elemento("form-movimiento").addEventListener("submit", function (e) {
  tareaFormulario(e, async function () {
    const tipo = elemento("movimiento-tipo").value;
    const origen = tipo === "ENTRADA" ? null : Number(elemento("movimiento-origen").value);
    const destino = tipo === "SALIDA" ? null : Number(elemento("movimiento-destino").value);
    if (tipo === "TRANSFERENCIA" && origen === destino) throw new Error("Selecciona bodegas de origen y destino diferentes.");
    const detalles = Array.from(elemento("detalles-movimiento").children).map(function (fila) {
      return { productoId: Number(fila.querySelector(".detalle-producto").value), cantidad: Number(fila.querySelector(".detalle-cantidad").value) };
    });
    const unicos = new Set(detalles.map(d => d.productoId));
    if (unicos.size !== detalles.length) throw new Error("No repitas un producto. Escribe su cantidad total en una sola fila.");
    const guardado = await peticion("/movimientos", "POST", { tipo: tipo, origenId: origen, destinoId: destino, detalles: detalles });
    elemento("modal-movimiento").close();
    notificar("Movimiento #" + guardado.id + " registrado por " + guardado.usuario + ".");
    elemento("form-filtro-fechas").reset();
    await cambiarVista("movimientos");
  });
});

// REPORTES Y AUDITORIAS
async function cargarReportes() {
  const resumen = await peticion("/reportes/resumen");
  tabla("tabla-reporte-bodegas", ["ID", "Bodega", "Stock total"], resumen.stockPorBodega.map(b => "<tr>" + celda(b.bodegaId) + celda(b.nombre, "nombre-celda") + celda(numero(b.stockTotal)) + "</tr>"));
  tabla("tabla-reporte-productos", ["ID", "Producto", "Unidades movidas"], resumen.productosMasMovidos.map(p => "<tr>" + celda(p.productoId) + celda(p.nombre, "nombre-celda") + celda(numero(p.cantidadMovida)) + "</tr>"));
}
async function cargarAuditorias() {
  const filtros = new URLSearchParams();
  const usuario = elemento("auditoria-usuario").value.trim();
  const operacion = elemento("auditoria-operacion").value;
  if (usuario) filtros.set("usuario", usuario);
  if (operacion) filtros.set("operacion", operacion);
  auditorias = await peticion("/auditorias?" + filtros);
  const filas = auditorias.map(a => "<tr>" + celda("#" + a.id) + celda(fecha(a.fecha)) + "<td>" + insignia(a.operacion) + "</td>" + celda(a.usuario) + celda(a.entidad + " #" + a.entidadId) + '<td><div class="acciones-tabla">' + accion("ver-auditoria", "Ver cambio", a.id) + "</div></td></tr>");
  tabla("tabla-auditorias", ["ID", "Fecha", "Operación", "Usuario", "Registro", "Detalle"], filas);
  elemento("contador-auditorias").textContent = auditorias.length + " cambios encontrados";
}
function jsonLegible(valor) {
  if (valor == null) return "Sin valores";
  try { return JSON.stringify(JSON.parse(valor), null, 2); }
  catch (error) { return valor; }
}
function verAuditoria(id) {
  const a = auditorias.find(a => a.id === id);
  elemento("titulo-auditoria").textContent = a.entidad + " #" + a.entidadId;
  elemento("descripcion-auditoria").textContent = a.usuario + " · " + fecha(a.fecha) + " · " + a.operacion;
  elemento("auditoria-antes").textContent = jsonLegible(a.valoresAnteriores);
  elemento("auditoria-despues").textContent = jsonLegible(a.valoresNuevos);
  abrirModal("modal-auditoria");
}
elemento("form-filtro-auditorias").addEventListener("submit", function (e) { e.preventDefault(); cambiarVista("auditorias"); });
elemento("limpiar-auditorias").addEventListener("click", function () { elemento("form-filtro-auditorias").reset(); cambiarVista("auditorias"); });

// Un solo listener para los botones de las tablas y los botones generales.
document.addEventListener("click", async function (e) {
  const cerrar = e.target.closest("[data-cerrar]");
  if (cerrar) { elemento(cerrar.dataset.cerrar).close(); return; }
  const navegar = e.target.closest("[data-ir]");
  if (navegar) { cambiarVista(navegar.dataset.ir); return; }
  const boton = e.target.closest("[data-accion]");
  if (!boton || boton.disabled) return;
  boton.disabled = true;
  const id = Number(boton.dataset.id);
  try {
    switch (boton.dataset.accion) {
      case "nueva-bodega": abrirBodega(); break;
      case "editar-bodega": abrirBodega(id); break;
      case "eliminar-bodega": confirmarEliminar("bodegas", id); break;
      case "inventario": await verInventario(id); break;
      case "nuevo-producto": abrirProducto(); break;
      case "editar-producto": abrirProducto(id); break;
      case "eliminar-producto": confirmarEliminar("productos", id); break;
      case "nuevo-movimiento": await abrirMovimiento(); break;
      case "ver-auditoria": verAuditoria(id); break;
    }
  } catch (error) { notificar(error.message, true); }
  finally { boton.disabled = false; }
});

// Recuperar la sesion de esta pestaña no altera los permisos del servidor.
if (sesion && sesion.token && sesion.username) {
  mostrarAplicacion();
  cambiarVista("inicio");
} else {
  borrarSesion();
}
