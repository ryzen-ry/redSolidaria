package com.redsolidaria.enjambre.model;

import lombok.Data;

@Data
public class RegistroTemporalVoluntario {
    private String nombres;
    private String apellidos;
    private String email;
    private String codigo;
    private String carrera;
    private String password;
    private String fotoPerfilPath;           // ✅ NUEVO - Ruta de la foto de perfil
    private String certificadoLaboralPath;   // ✅ NUEVO - Ruta del Certificado Único Laboral
}