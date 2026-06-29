package com.conectatarot.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "disputa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sesion_id", nullable = false)
    private Sesion sesion;

    @Column(name = "reportado_por", nullable = false)
    private String reportadoPor;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 500)
    private String resolucion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) this.estado = "PENDIENTE";
    }
}
