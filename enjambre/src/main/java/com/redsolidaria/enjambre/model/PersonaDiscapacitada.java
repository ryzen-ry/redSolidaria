package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "personas_discapacitadas")
public class PersonaDiscapacitada extends Usuario {

    @Column(unique = true, nullable = false)
    private String conadis;

    @Column(name = "tipo_discapacidad", nullable = false)
    private String tipoDiscapacidad;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String direccion;

    public PersonaDiscapacitada(String nombres, String apellidos, String email, String password,
                               String conadis, String tipoDiscapacidad, String telefono, String direccion) {
        super(nombres, apellidos, email, password, "DISCAPACITADO");
        this.conadis = conadis;
        this.tipoDiscapacidad = tipoDiscapacidad;
        this.telefono = telefono;
        this.direccion = direccion;
    }
}