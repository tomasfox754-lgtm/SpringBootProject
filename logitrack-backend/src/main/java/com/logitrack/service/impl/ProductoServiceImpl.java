package com.logitrack.service.impl;

import com.logitrack.dto.request.ProductoDtoRequest;
import com.logitrack.dto.response.*;
import com.logitrack.exception.*;
import com.logitrack.mapper.ProductoMapper;
import com.logitrack.model.Producto;
import com.logitrack.repository.*;
import com.logitrack.service.ProductoService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final InventarioRepository inventarioRepository;
    private final DetalleMovimientoRepository detalleMovimientoRepository;

    @Override
    @Transactional
    public ProductoDtoResponse guardar(ProductoDtoRequest dto) {
        Producto entidad = productoMapper.DtoToEntity(dto);
        Producto guardado = productoRepository.save(entidad);
        return productoMapper.entityToDto(guardado);
    }

    @Override
    @Transactional
    public ProductoDtoResponse actualizar(ProductoDtoRequest dto, Long id) {
        Producto entidad =
                productoRepository
                        .buscarParaActualizar(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Producto no encontrado: " + id));

        productoMapper.updateEntityFromDto(entidad, dto);
        return productoMapper.entityToDto(productoRepository.save(entidad));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto entidad =
                productoRepository
                        .buscarParaActualizar(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Producto no encontrado: " + id));
        if (inventarioRepository.existsByProductoId(id)
                || detalleMovimientoRepository.existsByProductoId(id))
            throw new ReglaNegocioException("El producto tiene inventario o movimientos asociados");
        productoRepository.delete(entidad);
    }

    @Override
    public List<ProductoDtoResponse> listar() {
        return productoRepository.findAll().stream().map(productoMapper::entityToDto).toList();
    }

    @Override
    public ProductoDtoResponse buscarId(Long id) {
        Producto entidad =
                productoRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Producto no encontrado: " + id));
        return productoMapper.entityToDto(entidad);
    }

    @Override
    public List<ProductoDtoResponse> stockBajo() {
        return productoRepository.findByStockLessThan(10L).stream()
                .map(productoMapper::entityToDto)
                .toList();
    }
}
