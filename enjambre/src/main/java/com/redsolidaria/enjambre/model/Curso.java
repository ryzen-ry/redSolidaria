package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private String nivel; // Básico, Intermedio, Avanzado

    @Column(name = "duracion_horas", nullable = false)
    private int duracionHoras;

    @Column(name = "url_video", nullable = false)
    private String urlVideo;

    @Column(nullable = false)
    private boolean activo = true;

    public Curso(String titulo, String descripcion, String nivel, int duracionHoras, String urlVideo) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.duracionHoras = duracionHoras;
        this.urlVideo = urlVideo;
        this.activo = true;
    }
}
