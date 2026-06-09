package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "solicitudes_ayuda")
public class SolicitudAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discapacitado_id", nullable = false)
    private PersonaDiscapacitada discapacitado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voluntario_aceptado_id")
    private Voluntario voluntarioAceptado;

    @Column(name = "estado", nullable = false)
    private String estado; // PENDIENTE | ACEPTADA | CANCELADA

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "aceptada_en")
    private LocalDateTime aceptadaEn;
}

