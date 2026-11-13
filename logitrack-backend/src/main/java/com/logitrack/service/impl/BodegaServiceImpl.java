package com.logitrack.service.impl;

import com.logitrack.dto.request.BodegaDtoRequest;
import com.logitrack.dto.response.*;
import com.logitrack.exception.*;
import com.logitrack.mapper.BodegaMapper;
import com.logitrack.model.Bodega;
import com.logitrack.repository.*;
import com.logitrack.service.BodegaService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BodegaServiceImpl implements BodegaService {
    private final BodegaRepository bodegaRepository;
    private final BodegaMapper bodegaMapper;
    private final InventarioRepository inventarioRepository;
    private final MovimientoRepository movimientoRepository;

    @Override
    @Transactional
    public BodegaDtoResponse guardar(BodegaDtoRequest dto) {
        Bodega entidad = bodegaMapper.DtoToEntity(dto);
        Bodega guardado = bodegaRepository.save(entidad);
        return bodegaMapper.entityToDto(guardado);
    }

    @Override
    @Transactional
    public BodegaDtoResponse actualizar(BodegaDtoRequest dto, Long id) {
        Bodega entidad =
                bodegaRepository
                        .buscarParaActualizar(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Bodega no encontrado: " + id));
        if (dto.capacidad() < inventarioRepository.stockEnBodega(id))
            throw new ReglaNegocioException("La capacidad no puede ser menor al stock almacenado");
        bodegaMapper.updateEntityFromDto(entidad, dto);
        return bodegaMapper.entityToDto(bodegaRepository.save(entidad));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Bodega entidad =
                bodegaRepository
                        .buscarParaActualizar(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Bodega no encontrado: " + id));
        if (inventarioRepository.existsByBodegaId(id)
                || movimientoRepository.existsByOrigenIdOrDestinoId(id, id))
            throw new ReglaNegocioException("La bodega tiene inventario o movimientos asociados");
        bodegaRepository.delete(entidad);
    }

    @Override
    public List<BodegaDtoResponse> listar() {
        return bodegaRepository.findAll().stream().map(bodegaMapper::entityToDto).toList();
    }

    @Override
    public BodegaDtoResponse buscarId(Long id) {
        Bodega entidad =
                bodegaRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "Bodega no encontrado: " + id));
        return bodegaMapper.entityToDto(entidad);
    }

    @Override
    public List<InventarioDtoResponse> inventario(Long id) {
        buscarId(id);
        return inventarioRepository.findByBodegaId(id).stream()
                .map(
                        i ->
                                new InventarioDtoResponse(
                                        id,
                                        i.getProducto().getId(),
                                        i.getProducto().getNombre(),
                                        i.getCantidad()))
                .toList();
    }
}
