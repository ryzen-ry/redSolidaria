package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.model.PersonaDiscapacitada;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.model.UsuarioBloqueado;
import com.redsolidaria.enjambre.model.Voluntario;
import com.redsolidaria.enjambre.repository.PersonaDiscapacitadaRepository;
import com.redsolidaria.enjambre.repository.UsuarioBloqueadoRepository;
import com.redsolidaria.enjambre.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioBloqueadoService {

    @Autowired
    private UsuarioBloqueadoRepository usuarioBloqueadoRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private PersonaDiscapacitadaRepository personaDiscapacitadaRepository;

    public void registrarBloqueo(Usuario usuario, String motivo, Long administradorId) {
        if (usuario == null) {
            return;
        }
        Usuario usuarioCompleto = resolverUsuarioCompleto(usuario);
        UsuarioBloqueado bloqueado = new UsuarioBloqueado(usuarioCompleto, motivo, administradorId);
        usuarioBloqueadoRepository.save(bloqueado);
    }

    private Usuario resolverUsuarioCompleto(Usuario usuario) {
        if ("VOLUNTARIO".equals(usuario.getRol())) {
            return voluntarioRepository.findById(usuario.getId()).orElse(usuario);
        }
        if ("DISCAPACITADO".equals(usuario.getRol())) {
            return personaDiscapacitadaRepository.findById(usuario.getId()).orElse(usuario);
        }
        return usuario;
    }

    public boolean estaEmailBloqueado(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return usuarioBloqueadoRepository.existsByEmailIgnoreCase(email.trim());
    }

    public boolean estaCodigoVoluntarioBloqueado(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        return usuarioBloqueadoRepository.existsByCodigoEstudianteIgnoreCase(codigo.trim());
    }

    public boolean estaDniBloqueado(String dni) {
        if (dni == null || dni.isBlank()) {
            return false;
        }
        return usuarioBloqueadoRepository.existsByDni(dni.trim());
    }

    public boolean estaCertificadoBloqueado(String certificado) {
        if (certificado == null || certificado.isBlank()) {
            return false;
        }
        return usuarioBloqueadoRepository.existsByCertificadoDiscapacidad(certificado.trim());
    }

    public String mensajeBloqueoVoluntario(String email, String codigo) {
        if (estaEmailBloqueado(email)) {
            return "⛔ No puedes registrarte. Este correo institucional pertenece a una cuenta suspendida permanentemente.";
        }
        if (estaCodigoVoluntarioBloqueado(codigo)) {
            return "⛔ No puedes registrarte. Este código de estudiante pertenece a una cuenta suspendida permanentemente.";
        }
        return null;
    }

    public String mensajeBloqueoDiscapacitado(String email, String dni, String certificado) {
        if (estaEmailBloqueado(email)) {
            return "⛔ No puedes registrarte. Este correo pertenece a una cuenta suspendida permanentemente.";
        }
        if (estaDniBloqueado(dni)) {
            return "⛔ No puedes registrarte. Este Nro. de DNI pertenece a una cuenta suspendida permanentemente.";
        }
        if (estaCertificadoBloqueado(certificado)) {
            return "⛔ No puedes registrarte. Este Nro. de certificado de discapacidad pertenece a una cuenta suspendida permanentemente.";
        }
        return null;
    }
}
