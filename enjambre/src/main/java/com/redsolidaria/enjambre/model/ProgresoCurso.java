package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "progreso_cursos",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"voluntario_id", "curso_id"})
    }
)
public class ProgresoCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voluntario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Voluntario voluntario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(name = "video_completado", nullable = false)
    private boolean videoCompletado = false;

    @Column(name = "fecha_inicio_video")
    private LocalDateTime fechaInicioVideo;

    @Column(name = "fecha_fin_video")
    private LocalDateTime fechaFinVideo;

    @Column(name = "puntaje_obtenido")
    private Integer puntajeObtenido;

    @Column(nullable = false)
    private boolean aprobado = false;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(name = "fecha_ultimo_intento")
    private LocalDateTime fechaUltimoIntento;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(nullable = false)
    private String estado = "EN_PROGRESO"; // EN_PROGRESO, COMPLETADO, APROBADO, REPROBADO, CANCELADO

    public ProgresoCurso(Voluntario voluntario, Curso curso) {
        this.voluntario = voluntario;
        this.curso = curso;
        this.videoCompletado = false;
        this.aprobado = false;
        this.intentos = 0;
        this.estado = "EN_PROGRESO";
    }
}
