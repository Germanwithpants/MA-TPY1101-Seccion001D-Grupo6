package com.conectatarot.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TarotistaResponseDTO {

    private Integer id;
    private String nombreProfesional;
    private String descripcion;
    private BigDecimal precioBase;
    private String estado;
    private List<String> especialidades;
    private Double promedio;
    private Integer totalResenas;
}