# Verificacion del frontend y su conexion

- JavaScript validado con `node --check` (config, API y pantallas).
- HTML: IDs unicos, etiquetas asociadas y referencias del JavaScript a elementos existentes.
- Backend Spring Boot: 17 pruebas de integracion correctas, incluidas las nuevas pruebas de CORS.
- Modulo `js/api.js`: 14 comprobaciones contra el backend real en un perfil demo aislado, sin usar datos del usuario.

Se comprobo login, JWT sin roles, preflight CORS de Live Server, CRUD, entrada con varios productos, stock insuficiente, transferencia, reportes, auditorias, registro de empleado, restricciones 403 y limpieza de sesion ante 401. Los resultados estan en `verificacion-api.json` y `pruebas-backend-cors.txt`.

La herramienta de navegador requerida por el entorno no estaba disponible. No se ejecutaron interacciones ni una revision visual automatizada del HTML en un navegador. La verificacion de API si se realizo con Spring Boot real.

Para una prueba visual en tu computador:

1. Inicia el backend con MySQL y abre el frontend con Live Server segun `LEEME_PRIMERO.md`.
2. Inicia sesion como admin y revisa Resumen, Bodegas, Productos y Reportes.
3. Crea un producto y una bodega de prueba; editalos y eliminalos antes de registrar inventario.
4. Registra una entrada de una unidad de un producto existente en una bodega existente y revisa el inventario.
5. Intenta retirar mas unidades de las disponibles: el formulario debe mostrar el error y no modificar stock.
6. Revisa Movimientos y filtra fechas; consulta las auditorias del admin.
7. Cierra sesion, entra como empleado1 y verifica que no aparezcan acciones de administracion, pero si pueda registrar movimientos.
8. Reduce el ancho de la ventana para verificar la distribucion en celular.

Toda accion guardada desde el frontend afecta la misma base de datos que utiliza Swagger.
