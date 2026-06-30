package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sanciones")
public class Sancion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historial_ayuda_id")
    private HistorialAyuda historialAyuda;

    @Column(name = "tipo_sancion", nullable = false)
    private String tipoSancion; // "AVISO_1", "AVISO_2", "BLOQUEO"

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fecha_sancion", nullable = false)
    private LocalDateTime fechaSancion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id")
    private Administrador administrador;

    public Sancion(Usuario usuario, HistorialAyuda historialAyuda, String tipoSancion, String motivo, Administrador administrador) {
        this.usuario = usuario;
        this.historialAyuda = historialAyuda;
        this.tipoSancion = tipoSancion;
        this.motivo = motivo;
        this.administrador = administrador;
        this.fechaSancion = LocalDateTime.now();
    }
}
