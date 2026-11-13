-- Catalogo inicial sin stock. Los cambios de inventario se hacen por /movimientos.
-- Se insertan solo si los nombres no existen, para permitir reinicios.
INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
 SELECT 'Bodega Bogota', 'Bogota', 1000, 'Ana Perez'
 WHERE NOT EXISTS (SELECT 1 FROM bodega WHERE nombre = 'Bodega Bogota');
INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
 SELECT 'Bodega Medellin', 'Medellin', 800, 'Luis Gomez'
 WHERE NOT EXISTS (SELECT 1 FROM bodega WHERE nombre = 'Bodega Medellin');
INSERT INTO producto (nombre, categoria, stock, precio)
 SELECT 'Teclado', 'Tecnologia', 0, 85000.00
 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Teclado');
INSERT INTO producto (nombre, categoria, stock, precio)
 SELECT 'Mouse', 'Tecnologia', 0, 45000.00
 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Mouse');
