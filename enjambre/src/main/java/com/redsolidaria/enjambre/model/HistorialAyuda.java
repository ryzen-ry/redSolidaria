package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_ayudas")
public class HistorialAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudAyuda solicitud;

    @Column(name = "fecha_finalizacion", nullable = false)
    private LocalDateTime fechaFinalizacion = LocalDateTime.now();

    // Calificación asignada por el discapacitado: "Oro", "Plata", "Cobre" o null
    @Column(name = "calificacion")
    private String calificacion;

    // Comentario del discapacitado hacia el voluntario
    @Column(name = "comentario_discapacitado", columnDefinition = "TEXT")
    private String comentarioDiscapacitado;

    // Comentario del voluntario hacia el discapacitado
    @Column(name = "comentario_voluntario", columnDefinition = "TEXT")
    private String comentarioVoluntario;

    // Flags para saber si ya comentaron (evitar doble comentario)
    @Column(name = "comentado_discapacitado", nullable = false)
    private boolean comentadoDiscapacitado = false;

    @Column(name = "comentado_voluntario", nullable = false)
    private boolean comentadoVoluntario = false;

    public HistorialAyuda(SolicitudAyuda solicitud) {
        this.solicitud = solicitud;
        this.fechaFinalizacion = LocalDateTime.now();
        this.comentadoDiscapacitado = false;
        this.comentadoVoluntario = false;
    }
}
