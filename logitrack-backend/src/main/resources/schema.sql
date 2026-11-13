-- Se ejecuta al iniciar. IF NOT EXISTS conserva los datos entre reinicios.
CREATE TABLE IF NOT EXISTS usuario (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(50) NOT NULL UNIQUE,
 password VARCHAR(100) NOT NULL,
 rol VARCHAR(20) NOT NULL,
 CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'EMPLEADO'))
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
 CONSTRAINT uq_inventario UNIQUE (bodega_id, producto_id),
 CONSTRAINT fk_inventario_bodega FOREIGN KEY (bodega_id) REFERENCES bodega(id),
 CONSTRAINT fk_inventario_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
 CONSTRAINT chk_cantidad CHECK (cantidad >= 0)
);
CREATE TABLE IF NOT EXISTS movimiento (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 fecha DATETIME(6) NOT NULL,
 tipo VARCHAR(20) NOT NULL,
 usuario_id BIGINT NOT NULL,
 origen_id BIGINT,
 destino_id BIGINT,
 CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
 CONSTRAINT fk_mov_origen FOREIGN KEY (origen_id) REFERENCES bodega(id),
 CONSTRAINT fk_mov_destino FOREIGN KEY (destino_id) REFERENCES bodega(id),
 CONSTRAINT chk_movimiento CHECK (
  (tipo = 'ENTRADA' AND origen_id IS NULL AND destino_id IS NOT NULL) OR
  (tipo = 'SALIDA' AND origen_id IS NOT NULL AND destino_id IS NULL) OR
  (tipo = 'TRANSFERENCIA' AND origen_id IS NOT NULL AND destino_id IS NOT NULL AND origen_id <> destino_id)
 )
);
CREATE TABLE IF NOT EXISTS detalle_movimiento (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 movimiento_id BIGINT NOT NULL,
 producto_id BIGINT NOT NULL,
 cantidad INT NOT NULL,
 CONSTRAINT fk_detalle_movimiento FOREIGN KEY (movimiento_id) REFERENCES movimiento(id),
 CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
 CONSTRAINT uq_detalle UNIQUE (movimiento_id, producto_id),
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
 CONSTRAINT chk_operacion CHECK (operacion IN ('INSERT', 'UPDATE', 'DELETE'))
);
