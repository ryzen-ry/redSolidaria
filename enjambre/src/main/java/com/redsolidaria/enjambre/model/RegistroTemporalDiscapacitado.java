package com.redsolidaria.enjambre.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RegistroTemporalDiscapacitado {
    private String nombres;
    private String apellidos;
    private String email;
    private String conadis;
    private String certificadoDiscapacidad;
    private String tipoDiscapacidad;
    private String telefono;
    private String direccion;
    private String password;
    private String dniDelanteraPath;
    private String dniTraseraPath;
    private String conadisFotoPath;
}