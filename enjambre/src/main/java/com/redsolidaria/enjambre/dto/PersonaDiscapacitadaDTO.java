package com.redsolidaria.enjambre.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PersonaDiscapacitadaDTO {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 50, message = "Los nombres deben tener entre 2 y 50 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    private String apellidos;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ingresar un correo electrónico válido")
    private String email;

    @NotBlank(message = "El número de DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El número de DNI debe tener exactamente 8 dígitos")
    private String conadis;

    @NotBlank(message = "El número de certificado de discapacidad es obligatorio")
    @Size(min = 3, max = 50, message = "El certificado debe tener entre 3 y 50 caracteres")
    private String certificadoDiscapacidad;

    @NotBlank(message = "Debe seleccionar el tipo de discapacidad")
    private String tipoDiscapacidad;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos")
    private String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 5, max = 100, message = "La dirección debe tener entre 5 y 100 caracteres")
    private String direccion;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 caracteres")
    private String password;

    @NotBlank(message = "Debe confirmar su contraseña")
    private String confirmPassword;

    // Campos para las fotos
    private MultipartFile dniFotoDelantera;
    private MultipartFile dniFotoTrasera;
    private MultipartFile conadisFoto;

    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}