package com.redsolidaria.enjambre.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "usuarios_bloqueados")
public class UsuarioBloqueado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private String rol;

    @Column(nullable = false)
    private String email;

    private String nombres;

    private String apellidos;

    @Column(name = "codigo_estudiante")
    private String codigoEstudiante;

    private String dni;

    @Column(name = "certificado_discapacidad")
    private String certificadoDiscapacidad;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fecha_bloqueo", nullable = false)
    private LocalDateTime fechaBloqueo = LocalDateTime.now();

    @Column(name = "administrador_id")
    private Long administradorId;

    public UsuarioBloqueado(Usuario usuario, String motivo, Long administradorId) {
        this.usuarioId = usuario.getId();
        this.rol = usuario.getRol();
        this.email = usuario.getEmail().toLowerCase();
        this.nombres = usuario.getNombres();
        this.apellidos = usuario.getApellidos();
        this.motivo = motivo;
        this.administradorId = administradorId;
        this.fechaBloqueo = LocalDateTime.now();

        if (usuario instanceof Voluntario vol) {
            this.codigoEstudiante = vol.getCodigo() != null ? vol.getCodigo().toUpperCase() : null;
        } else if (usuario instanceof PersonaDiscapacitada disc) {
            this.dni = disc.getConadis();
            this.certificadoDiscapacidad = disc.getCertificadoDiscapacidad();
        }
    }
}
