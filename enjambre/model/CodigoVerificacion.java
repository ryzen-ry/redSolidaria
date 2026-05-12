package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    private String codigo;
    private LocalDateTime fechaExpiracion;
    private boolean usado;
    
    public CodigoVerificacion() {}
    
    public CodigoVerificacion(String email, String codigo, LocalDateTime fechaExpiracion) {
        this.email = email;
        this.codigo = codigo;
        this.fechaExpiracion = fechaExpiracion;
        this.usado = false;
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}