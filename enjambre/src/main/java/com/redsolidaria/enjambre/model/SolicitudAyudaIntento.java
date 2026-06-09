package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(
        name = "solicitudes_ayuda_intentos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solicitud_voluntario",
                columnNames = {"solicitud_ayuda_id", "voluntario_id"}
        )
)
public class SolicitudAyudaIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_ayuda_id", nullable = false)
    private SolicitudAyuda solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voluntario_id", nullable = false)
    private Voluntario voluntario;

    @Column(name = "estado", nullable = false)
    private String estado; // PENDIENTE | RECHAZADA | ACEPTADA

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "respondida_en")
    private LocalDateTime respondidaEn;
}

