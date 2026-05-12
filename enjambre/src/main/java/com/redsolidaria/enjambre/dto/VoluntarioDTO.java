package com.redsolidaria.enjambre.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VoluntarioDTO {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 50, message = "Los nombres deben tener entre 2 y 50 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    private String apellidos;

    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "Debe ingresar un correo electrónico válido")
    @Pattern(regexp = "^U\\d{8}@utp\\.edu\\.pe$", message = "El correo debe ser institucional UTP (ej: U12345678@utp.edu.pe)")
    private String email;

    @NotBlank(message = "El código de estudiante es obligatorio")
    @Pattern(regexp = "^U\\d{8}$", message = "El código debe comenzar con U seguido de 8 dígitos")
    private String codigo;

    @NotBlank(message = "Debe seleccionar una carrera")
    private String carrera;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 caracteres")
    private String password;

    @NotBlank(message = "Debe confirmar su contraseña")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}