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
@Table(name = "donaciones_productos")
public class DonacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_producto", nullable = false)
    private String tipoProducto;

    @Column(name = "estado_producto", nullable = false)
    private String estadoProducto;

    @Transient
    private String nombreCompleto;

    @Transient
    private String email;

    @Column(nullable = false)
    private String telefono;

    @Column(name = "opcion_entrega", nullable = false)
    private String opcionEntrega; // "recoger" o "llevar"

    @Column(columnDefinition = "TEXT")
    private String direccion; // nullable

    private String horario; // nullable

    @Column(columnDefinition = "TEXT")
    private String comentarios; // nullable

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // "PENDIENTE", "CONFIRMADO", "RECHAZADO"

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
