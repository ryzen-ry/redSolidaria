package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "administradores")
public class Administrador extends Usuario {

    @Column(name = "nivel_acceso")
    private String nivelAcceso = "TOTAL";  // TOTAL, MEDIO, BASICO
    
    @Column(name = "fecha_creacion")
    private String fechaCreacion;

    public Administrador(String nombres, String apellidos, String email, String password) {
        super(nombres, apellidos, email, password, "ADMIN");
        this.nivelAcceso = "TOTAL";
        super.setVerificado(true);  // Admin siempre verificado
    }
}