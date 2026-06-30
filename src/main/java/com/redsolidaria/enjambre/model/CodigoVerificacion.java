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
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String codigo;
    private LocalDateTime fechaExpiracion;
    private boolean usado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public CodigoVerificacion(String email, String codigo, LocalDateTime fechaExpiracion) {
        this.email = email;
        this.codigo = codigo;
        this.fechaExpiracion = fechaExpiracion;
        this.usado = false;
    }
} 