package com.logitrack.service.impl;

import com.logitrack.dto.request.MovimientoDtoRequest;
import com.logitrack.dto.response.MovimientoDtoResponse;
import com.logitrack.exception.*;
import com.logitrack.mapper.MovimientoMapper;
import com.logitrack.model.*;
import com.logitrack.repository.*;
import com.logitrack.service.MovimientoService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovimientoServiceImpl implements MovimientoService {
    private final MovimientoRepository movimientoRepository;
    private final BodegaRepository bodegaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoMapper movimientoMapper;

    @Override
    @Transactional
    public MovimientoDtoResponse guardar(MovimientoDtoRequest dto, String username) {
        validarBodegas(dto);
        Set<Long> productosUnicos = new HashSet<>();
        for (var detalle : dto.detalles()) {
            if (!productosUnicos.add(detalle.productoId()))
                throw new ReglaNegocioException("No repitas un producto en el mismo movimiento");
        }
        // Bloquear siempre de menor a mayor ID evita cruces entre transferencias concurrentes.
        List<Long> idsBodegas = new ArrayList<>();
        if (dto.origenId() != null) idsBodegas.add(dto.origenId());
        if (dto.destinoId() != null) idsBodegas.add(dto.destinoId());
        Collections.sort(idsBodegas);
        Map<Long, Bodega> bodegas = new HashMap<>();
        for (Long id : idsBodegas) {
            Bodega b =
                    bodegaRepository
                            .buscarParaActualizar(id)
                            .orElseThrow(
                                    () ->
                                            new RecursoNoEncontradoException(
                                                    "Bodega no encontrada: " + id));
            bodegas.put(id, b);
        }
        Bodega origen = bodegas.get(dto.origenId());
        Bodega destino = bodegas.get(dto.destinoId());
        long total = 0;
        for (var detalle : dto.detalles()) total += detalle.cantidad();
        if (destino != null
                && inventarioRepository.stockEnBodega(destino.getId()) + total
                        > destino.getCapacidad())
            throw new ReglaNegocioException("La bodega destino no tiene capacidad suficiente");
        Map<Long, Producto> productos = new HashMap<>();
        for (Long id : productosUnicos.stream().sorted().toList()) {
            Producto p =
                    productoRepository
                            .buscarParaActualizar(id)
                            .orElseThrow(
                                    () ->
                                            new RecursoNoEncontradoException(
                                                    "Producto no encontrado: " + id));
            productos.put(id, p);
        }
        Movimiento movimiento = new Movimiento();
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo(dto.tipo());
        movimiento.setUsuario(
                usuarioRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Usuario no encontrado")));
        movimiento.setOrigen(origen);
        movimiento.setDestino(destino);
        for (var detalleDto : dto.detalles()) {
            Producto producto = productos.get(detalleDto.productoId());
            int cantidad = detalleDto.cantidad();
            if (origen != null) cambiarInventario(origen, producto, -cantidad);
            if (destino != null) cambiarInventario(destino, producto, cantidad);
            // Una transferencia cambia la distribucion, pero conserva el stock global.
            if (dto.tipo() == TipoMovimiento.ENTRADA)
                producto.setStock(producto.getStock() + cantidad);
            if (dto.tipo() == TipoMovimiento.SALIDA)
                producto.setStock(producto.getStock() - cantidad);
            DetalleMovimiento detalle = new DetalleMovimiento();
            detalle.setMovimiento(movimiento);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            movimiento.getDetalles().add(detalle);
        }
        Movimiento guardado = movimientoRepository.save(movimiento);
        return movimientoMapper.entityToDto(guardado);
    }

    private void cambiarInventario(Bodega bodega, Producto producto, int cambio) {
        Inventario inventario =
                inventarioRepository
                        .findByBodegaIdAndProductoId(bodega.getId(), producto.getId())
                        .orElse(null);
        if (inventario == null) {
            if (cambio < 0)
                throw new ReglaNegocioException(
                        "Stock insuficiente de "
                                + producto.getNombre()
                                + " en "
                                + bodega.getNombre());
            inventario = new Inventario();
            inventario.setBodega(bodega);
            inventario.setProducto(producto);
        }
        long nuevaCantidad = inventario.getCantidad() + cambio;
        if (nuevaCantidad < 0)
            throw new ReglaNegocioException(
                    "Stock insuficiente de " + producto.getNombre() + " en " + bodega.getNombre());
        inventario.setCantidad(nuevaCantidad);
        inventarioRepository.save(inventario);
    }

    private void validarBodegas(MovimientoDtoRequest dto) {
        if (dto.tipo() == TipoMovimiento.ENTRADA
                && (dto.origenId() != null || dto.destinoId() == null))
            throw new ReglaNegocioException("ENTRADA requiere solo destinoId");
        if (dto.tipo() == TipoMovimiento.SALIDA
                && (dto.origenId() == null || dto.destinoId() != null))
            throw new ReglaNegocioException("SALIDA requiere solo origenId");
        if (dto.tipo() == TipoMovimiento.TRANSFERENCIA
                && (dto.origenId() == null
                        || dto.destinoId() == null
                        || dto.origenId().equals(dto.destinoId())))
            throw new ReglaNegocioException(
                    "TRANSFERENCIA requiere origenId y destinoId diferentes");
    }

    @Override
    public MovimientoDtoResponse buscarId(Long id) {
        return movimientoMapper.entityToDto(
                movimientoRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Movimiento no encontrado: " + id)));
    }

    @Override
    public List<MovimientoDtoResponse> listar(LocalDateTime desde, LocalDateTime hasta) {
        if ((desde == null) != (hasta == null))
            throw new ReglaNegocioException("Envia desde y hasta juntos");
        if (desde != null && desde.isAfter(hasta))
            throw new ReglaNegocioException("Desde debe ser anterior o igual a hasta");
        List<Movimiento> movimientos =
                desde == null
                        ? movimientoRepository.findAllByOrderByFechaDesc()
                        : movimientoRepository.findByFechaBetweenOrderByFechaDesc(desde, hasta);
        return movimientos.stream().map(movimientoMapper::entityToDto).toList();
    }
}
