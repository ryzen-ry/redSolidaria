package com.redsolidaria.enjambre.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ComentarioDTO {

    private Long id;

    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(min = 1, max = 500, message = "El comentario debe tener entre 1 y 500 caracteres")
    private String contenido;
}