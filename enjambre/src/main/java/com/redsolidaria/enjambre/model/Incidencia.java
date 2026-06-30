package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historial_ayuda_id", nullable = false)
    private HistorialAyuda historialAyuda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciado_id", nullable = false)
    private Usuario denunciado;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(name = "evidencia_url")
    private String evidenciaUrl;

    @Column(name = "estado", nullable = false)
    private String estado = "PENDIENTE"; // "PENDIENTE", "EN_REVISION", "RESUELTO"

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
