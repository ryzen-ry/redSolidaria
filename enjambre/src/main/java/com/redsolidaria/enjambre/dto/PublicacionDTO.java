package com.redsolidaria.enjambre.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PublicacionDTO {

    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 3, max = 100, message = "El título debe tener entre 3 y 100 caracteres")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    @Size(min = 10, max = 2000, message = "El contenido debe tener entre 10 y 2000 caracteres")
    private String contenido;

    @NotBlank(message = "Debe seleccionar una categoría")
    private String categoria;
}