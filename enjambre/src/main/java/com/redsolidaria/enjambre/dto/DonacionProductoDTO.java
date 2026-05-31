package com.redsolidaria.enjambre.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DonacionProductoDTO {

    @NotBlank(message = "El tipo de producto es obligatorio")
    private String tipoProducto;

    @NotBlank(message = "El estado del producto es obligatorio")
    private String estadoProducto;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ingresar un correo electrónico válido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono de contacto debe tener 9 dígitos")
    private String telefono;

    @NotBlank(message = "La opción de entrega es obligatoria")
    private String opcionEntrega; // "recoger" o "llevar"

    private String direccion; // nullable

    private String horario; // nullable

    private String comentarios; // nullable
}
