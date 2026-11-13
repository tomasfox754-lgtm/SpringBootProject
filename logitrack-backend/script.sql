CREATE DATABASE IF NOT EXISTS logitrack
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE logitrack;

SET time_zone = '-05:00';

-- =====================================
-- TABLAS
-- =====================================

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,

    CONSTRAINT chk_usuario_rol
        CHECK (rol IN ('ADMIN', 'EMPLEADO'))
);

CREATE TABLE IF NOT EXISTS bodega (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    ubicacion VARCHAR(150) NOT NULL,
    capacidad BIGINT NOT NULL,
    encargado VARCHAR(100) NOT NULL,

    CONSTRAINT chk_capacidad CHECK (capacidad > 0)
);

CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    stock BIGINT NOT NULL DEFAULT 0,
    precio DECIMAL(12,2) NOT NULL,

    CONSTRAINT chk_stock CHECK (stock >= 0),
    CONSTRAINT chk_precio CHECK (precio > 0)
);

CREATE TABLE IF NOT EXISTS inventario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bodega_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_inventario
        UNIQUE (bodega_id, producto_id),

    CONSTRAINT fk_inventario_bodega
        FOREIGN KEY (bodega_id) REFERENCES bodega(id),

    CONSTRAINT fk_inventario_producto
        FOREIGN KEY (producto_id) REFERENCES producto(id),

    CONSTRAINT chk_cantidad CHECK (cantidad >= 0)
);

CREATE TABLE IF NOT EXISTS movimiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME(6) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    usuario_id BIGINT NOT NULL,
    origen_id BIGINT,
    destino_id BIGINT,

    CONSTRAINT fk_mov_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id),

    CONSTRAINT fk_mov_origen
        FOREIGN KEY (origen_id) REFERENCES bodega(id),

    CONSTRAINT fk_mov_destino
        FOREIGN KEY (destino_id) REFERENCES bodega(id),

    CONSTRAINT chk_movimiento CHECK (
        (
            tipo = 'ENTRADA'
            AND origen_id IS NULL
            AND destino_id IS NOT NULL
        )
        OR
        (
            tipo = 'SALIDA'
            AND origen_id IS NOT NULL
            AND destino_id IS NULL
        )
        OR
        (
            tipo = 'TRANSFERENCIA'
            AND origen_id IS NOT NULL
            AND destino_id IS NOT NULL
            AND origen_id <> destino_id
        )
    )
);

CREATE TABLE IF NOT EXISTS detalle_movimiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movimiento_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,

    CONSTRAINT fk_detalle_movimiento
        FOREIGN KEY (movimiento_id) REFERENCES movimiento(id),

    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (producto_id) REFERENCES producto(id),

    CONSTRAINT uq_detalle
        UNIQUE (movimiento_id, producto_id),

    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0)
);

CREATE TABLE IF NOT EXISTS auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operacion VARCHAR(10) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    usuario VARCHAR(50) NOT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_id BIGINT NOT NULL,
    valores_anteriores TEXT,
    valores_nuevos TEXT,

    CONSTRAINT chk_operacion
        CHECK (operacion IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- =====================================
-- USUARIO DE CONEXION DE SPRING BOOT
-- =====================================

CREATE USER IF NOT EXISTS 'logitrack'@'localhost'
IDENTIFIED BY 'LogitrackLocal123!';

ALTER USER 'logitrack'@'localhost'
IDENTIFIED BY 'LogitrackLocal123!';

GRANT ALL PRIVILEGES ON logitrack.*
TO 'logitrack'@'localhost';

START TRANSACTION;

-- =====================================
-- ADMINISTRADOR
-- =====================================

INSERT INTO usuario (username, password, rol)
VALUES (
    'admin',
    '$2a$10$9yulxzKm9I1HoOopy/OE5el.dKLEz05eB5hTsFp5aLtbeMiagvU6K',
    'ADMIN'
)
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    password = '$2a$10$9yulxzKm9I1HoOopy/OE5el.dKLEz05eB5hTsFp5aLtbeMiagvU6K',
    rol = 'ADMIN';

SET @u1 = LAST_INSERT_ID();

-- =====================================
-- TRES EMPLEADOS
-- =====================================

INSERT INTO usuario (username, password, rol)
VALUES (
    'empleado1',
    '$2a$10$98cpSsiy5yV9Uqy8CngzpuDKOBFois2oudWRGU/qVzqMfV9LCwuFW',
    'EMPLEADO'
)
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    password = '$2a$10$98cpSsiy5yV9Uqy8CngzpuDKOBFois2oudWRGU/qVzqMfV9LCwuFW',
    rol = 'EMPLEADO';

SET @u2 = LAST_INSERT_ID();

INSERT INTO usuario (username, password, rol)
VALUES (
    'empleado2',
    '$2a$10$98cpSsiy5yV9Uqy8CngzpuDKOBFois2oudWRGU/qVzqMfV9LCwuFW',
    'EMPLEADO'
)
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    password = '$2a$10$98cpSsiy5yV9Uqy8CngzpuDKOBFois2oudWRGU/qVzqMfV9LCwuFW',
    rol = 'EMPLEADO';

SET @u3 = LAST_INSERT_ID();

INSERT INTO usuario (username, password, rol)
VALUES (
    'empleado3',
    '$2a$10$98cpSsiy5yV9Uqy8CngzpuDKOBFois2oudWRGU/qVzqMfV9LCwuFW',
    'EMPLEADO'
)
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    password = '$2a$10$98cpSsiy5yV9Uqy8CngzpuDKOBFois2oudWRGU/qVzqMfV9LCwuFW',
    rol = 'EMPLEADO';

SET @u4 = LAST_INSERT_ID();

-- =====================================
-- CINCO BODEGAS Y SUS ENCARGADOS
-- =====================================

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Bogota', 'Bogota', 1000, 'Juan Perez'
WHERE NOT EXISTS (
    SELECT 1 FROM bodega WHERE nombre = 'Bodega Bogota'
);

SET @b1 = (
    SELECT MIN(id) FROM bodega WHERE nombre = 'Bodega Bogota'
);

UPDATE bodega SET encargado = 'Juan Perez' WHERE id = @b1;

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Medellin', 'Medellin', 800, 'Maria Lopez'
WHERE NOT EXISTS (
    SELECT 1 FROM bodega WHERE nombre = 'Bodega Medellin'
);

SET @b2 = (
    SELECT MIN(id) FROM bodega WHERE nombre = 'Bodega Medellin'
);

UPDATE bodega SET encargado = 'Maria Lopez' WHERE id = @b2;

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Cali', 'Cali', 600, 'Carlos Ruiz'
WHERE NOT EXISTS (
    SELECT 1 FROM bodega WHERE nombre = 'Bodega Cali'
);

SET @b3 = (
    SELECT MIN(id) FROM bodega WHERE nombre = 'Bodega Cali'
);

UPDATE bodega SET encargado = 'Carlos Ruiz' WHERE id = @b3;

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Barranquilla', 'Barranquilla', 500, 'Juan Perez'
WHERE NOT EXISTS (
    SELECT 1 FROM bodega WHERE nombre = 'Bodega Barranquilla'
);

SET @b4 = (
    SELECT MIN(id) FROM bodega WHERE nombre = 'Bodega Barranquilla'
);

UPDATE bodega SET encargado = 'Juan Perez' WHERE id = @b4;

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Bucaramanga', 'Bucaramanga', 400, 'Maria Lopez'
WHERE NOT EXISTS (
    SELECT 1 FROM bodega WHERE nombre = 'Bodega Bucaramanga'
);

SET @b5 = (
    SELECT MIN(id) FROM bodega WHERE nombre = 'Bodega Bucaramanga'
);

UPDATE bodega SET encargado = 'Maria Lopez' WHERE id = @b5;

-- =====================================
-- CINCO PRODUCTOS DE TECNOLOGIA
-- =====================================

INSERT INTO producto (nombre, categoria, stock, precio)
SELECT 'Teclado', 'Tecnologia', 0, 85000.00
WHERE NOT EXISTS (
    SELECT 1 FROM producto WHERE nombre = 'Teclado'
);

SET @p1 = (
    SELECT MIN(id) FROM producto WHERE nombre = 'Teclado'
);

INSERT INTO producto (nombre, categoria, stock, precio)
SELECT 'Mouse', 'Tecnologia', 0, 45000.00
WHERE NOT EXISTS (
    SELECT 1 FROM producto WHERE nombre = 'Mouse'
);

SET @p2 = (
    SELECT MIN(id) FROM producto WHERE nombre = 'Mouse'
);

INSERT INTO producto (nombre, categoria, stock, precio)
SELECT 'Monitor', 'Tecnologia', 0, 650000.00
WHERE NOT EXISTS (
    SELECT 1 FROM producto WHERE nombre = 'Monitor'
);

SET @p3 = (
    SELECT MIN(id) FROM producto WHERE nombre = 'Monitor'
);

INSERT INTO producto (nombre, categoria, stock, precio)
SELECT 'Memoria USB', 'Tecnologia', 0, 35000.00
WHERE NOT EXISTS (
    SELECT 1 FROM producto WHERE nombre = 'Memoria USB'
);

SET @p4 = (
    SELECT MIN(id) FROM producto WHERE nombre = 'Memoria USB'
);

INSERT INTO producto (nombre, categoria, stock, precio)
SELECT 'Audifonos', 'Tecnologia', 0, 120000.00
WHERE NOT EXISTS (
    SELECT 1 FROM producto WHERE nombre = 'Audifonos'
);

SET @p5 = (
    SELECT MIN(id) FROM producto WHERE nombre = 'Audifonos'
);

UPDATE producto
SET categoria = 'Tecnologia'
WHERE id IN (@p1, @p2, @p3, @p4, @p5);

-- =====================================
-- CINCO MOVIMIENTOS
-- =====================================

SET @fecha = DATE_SUB(NOW(6), INTERVAL 5 MINUTE);

INSERT INTO movimiento
(fecha, tipo, usuario_id, origen_id, destino_id)
VALUES (@fecha, 'ENTRADA', @u2, NULL, @b1);

SET @m1 = LAST_INSERT_ID();

INSERT INTO movimiento
(fecha, tipo, usuario_id, origen_id, destino_id)
VALUES (
    DATE_ADD(@fecha, INTERVAL 1 MINUTE),
    'ENTRADA', @u3, NULL, @b2
);

SET @m2 = LAST_INSERT_ID();

INSERT INTO movimiento
(fecha, tipo, usuario_id, origen_id, destino_id)
VALUES (
    DATE_ADD(@fecha, INTERVAL 2 MINUTE),
    'ENTRADA', @u4, NULL, @b3
);

SET @m3 = LAST_INSERT_ID();

INSERT INTO movimiento
(fecha, tipo, usuario_id, origen_id, destino_id)
VALUES (
    DATE_ADD(@fecha, INTERVAL 3 MINUTE),
    'ENTRADA', @u2, NULL, @b4
);

SET @m4 = LAST_INSERT_ID();

INSERT INTO movimiento
(fecha, tipo, usuario_id, origen_id, destino_id)
VALUES (
    DATE_ADD(@fecha, INTERVAL 4 MINUTE),
    'TRANSFERENCIA', @u2, @b1, @b5
);

SET @m5 = LAST_INSERT_ID();

-- =====================================
-- CINCO DETALLES DE MOVIMIENTO
-- =====================================

INSERT INTO detalle_movimiento
(movimiento_id, producto_id, cantidad)
VALUES
(@m1, @p1, 20),
(@m2, @p2, 15),
(@m3, @p3, 12),
(@m4, @p4, 8),
(@m5, @p1, 5);

-- =====================================
-- CINCO OPERACIONES DE INVENTARIO
-- Se suman a las existencias actuales.
-- =====================================

INSERT INTO inventario (bodega_id, producto_id, cantidad)
VALUES (@b1, @p1, 15)
ON DUPLICATE KEY UPDATE cantidad = cantidad + 15;

INSERT INTO inventario (bodega_id, producto_id, cantidad)
VALUES (@b2, @p2, 15)
ON DUPLICATE KEY UPDATE cantidad = cantidad + 15;

INSERT INTO inventario (bodega_id, producto_id, cantidad)
VALUES (@b3, @p3, 12)
ON DUPLICATE KEY UPDATE cantidad = cantidad + 12;

INSERT INTO inventario (bodega_id, producto_id, cantidad)
VALUES (@b4, @p4, 8)
ON DUPLICATE KEY UPDATE cantidad = cantidad + 8;

INSERT INTO inventario (bodega_id, producto_id, cantidad)
VALUES (@b5, @p1, 5)
ON DUPLICATE KEY UPDATE cantidad = cantidad + 5;

-- STOCK GLOBAL DE LOS PRODUCTOS
-- La transferencia conserva el total de teclados.

UPDATE producto SET stock = stock + 20 WHERE id = @p1;
UPDATE producto SET stock = stock + 15 WHERE id = @p2;
UPDATE producto SET stock = stock + 12 WHERE id = @p3;
UPDATE producto SET stock = stock + 8 WHERE id = @p4;

-- =====================================
-- CINCO AUDITORIAS DE LA CARGA
-- =====================================

INSERT INTO auditoria (
    operacion,
    fecha,
    usuario,
    entidad,
    entidad_id,
    valores_anteriores,
    valores_nuevos
)
SELECT
    'INSERT',
    m.fecha,
    u.username,
    'Movimiento',
    m.id,
    NULL,
    JSON_OBJECT(
        'id', m.id,
        'tipo', m.tipo,
        'usuario', u.username,
        'fecha', DATE_FORMAT(m.fecha, '%Y-%m-%dT%H:%i:%s'),
        'origenId', m.origen_id,
        'destinoId', m.destino_id,
        'detalles', JSON_ARRAY(
            JSON_OBJECT(
                'productoId', d.producto_id,
                'cantidad', d.cantidad
            )
        )
    )
FROM movimiento m
JOIN usuario u ON u.id = m.usuario_id
JOIN detalle_movimiento d ON d.movimiento_id = m.id
WHERE m.id IN (@m1, @m2, @m3, @m4, @m5);

COMMIT;

-- =====================================
-- COMPROBACIONES
-- =====================================

SELECT id, username, rol
FROM usuario
WHERE id IN (@u1, @u2, @u3, @u4);

SELECT id, nombre, ubicacion, encargado
FROM bodega
WHERE id IN (@b1, @b2, @b3, @b4, @b5);

SELECT
    p.id,
    p.nombre,
    p.categoria,
    p.stock,
    COALESCE(SUM(i.cantidad), 0) AS stock_en_bodegas
FROM producto p
LEFT JOIN inventario i ON i.producto_id = p.id
WHERE p.id IN (@p1, @p2, @p3, @p4, @p5)
GROUP BY p.id, p.nombre, p.categoria, p.stock;