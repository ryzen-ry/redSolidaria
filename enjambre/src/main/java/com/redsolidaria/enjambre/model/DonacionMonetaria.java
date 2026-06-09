package com.redsolidaria.enjambre.model;

import com.redsolidaria.enjambre.model.Usuario;
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

    @Transient
    private String nombreCompleto;

    @Column(nullable = false)
    private String celular;

    @Transient
    private String email;

    @Column(name = "codigo_yape")
    private String codigoYape;

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // "PENDIENTE", "VERIFICANDO", "CONFIRMADO", "RECHAZADO"

    @Column(name = "fecha_donacion", nullable = false)
    private LocalDateTime fechaDonacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public String getNombreCompleto() {
        return usuario != null ? usuario.getNombreCompleto() : nombreCompleto;
    }

    public String getEmail() {
        return usuario != null ? usuario.getEmail() : email;
    }
} 
