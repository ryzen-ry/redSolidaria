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
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "usuarios")
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "verificado")
    private boolean verificado = false;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "estado", nullable = false)
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    private String rol;

    public Usuario(String nombres, String apellidos, String email, String password, String rol) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.verificado = false;
        this.fechaRegistro = LocalDateTime.now();
        this.estado = "ADMIN".equals(rol) ? "ACTIVO" : "PENDIENTE";
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}