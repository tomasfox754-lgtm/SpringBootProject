package com.logitrack.dto.response;

import com.logitrack.model.*;

import java.util.List;

public record ResumenDtoResponse(
        List<StockBodegaDtoResponse> stockPorBodega,
        List<ProductoMovidoDtoResponse> productosMasMovidos) {}
