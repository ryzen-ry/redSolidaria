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
@Table(name = "publicaciones")
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    private String categoria;  // Experiencia, Consejo, Capacitación, Agradecimiento, Pregunta

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "likes")
    private int likes = 0;

    public Publicacion(String titulo, String contenido, String categoria, Usuario usuario) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.categoria = categoria;
        this.usuario = usuario;
        this.fechaCreacion = LocalDateTime.now();
        this.likes = 0;
    }
}