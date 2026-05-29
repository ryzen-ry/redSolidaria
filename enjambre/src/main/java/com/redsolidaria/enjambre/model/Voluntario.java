package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "voluntarios")
public class Voluntario extends Usuario {

    @Column(unique = true, nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String carrera;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "certificado_laboral")  // ✅ AGREGAR ESTA LÍNEA
    private String certificadoLaboral;      // ✅ AGREGAR ESTA LÍNEA

    @Column(name = "puntos")
    private int puntos = 0;

    public Voluntario(String nombres, String apellidos, String email, String password,
                     String codigo, String carrera) {
        super(nombres, apellidos, email, password, "VOLUNTARIO");
        this.codigo = codigo;
        this.carrera = carrera;
        this.puntos = 0;
    }
}