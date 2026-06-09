package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_activaciones")
public class HistorialActivacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id")
    private Usuario administrador;

    @Column(name = "fecha_activacion", nullable = false)
    private LocalDateTime fechaActivacion = LocalDateTime.now();

    @Column(nullable = false, length = 30)
    private String rol;

    /** Tabla específica del perfil: voluntarios | personas_discapacitadas */
    @Column(name = "tabla_origen", nullable = false, length = 50)
    private String tablaOrigen;
}
