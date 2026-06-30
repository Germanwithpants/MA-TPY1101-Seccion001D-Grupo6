package com.conectatarot.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SesionResponseDTO {

    private Integer id;
    private String nombreCliente;
    private String emailCliente;
    private String nombreTarotista;
    private String especialidad;
    private LocalDateTime fecha;
    private Integer duracionMinutos;
    private BigDecimal precioTotal;
    private String estado;
    private String estadoPago;
    private Integer tarotistaId;
    private LocalDateTime fechaCreacion;
}
