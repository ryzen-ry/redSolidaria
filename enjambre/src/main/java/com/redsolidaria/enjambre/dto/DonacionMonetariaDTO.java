package com.redsolidaria.enjambre.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DonacionMonetariaDTO {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.0", message = "El monto mínimo a donar es S/. 1.0")
    private Double monto;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El celular debe tener exactamente 9 dígitos")
    private String celular;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ingresar un correo electrónico válido")
    private String email;

    private String codigoYape;
}
