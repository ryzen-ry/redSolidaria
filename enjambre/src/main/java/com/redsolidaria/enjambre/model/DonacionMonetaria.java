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
@Table(name = "donaciones_monetarias")
public class DonacionMonetaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double monto;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private String celular;

    @Column(nullable = false)
    private String email;

    @Column(name = "codigo_yape")
    private String codigoYape;

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // "PENDIENTE", "VERIFICANDO", "CONFIRMADO", "RECHAZADO"

    @Column(name = "fecha_donacion", nullable = false)
    private LocalDateTime fechaDonacion = LocalDateTime.now();

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
}
