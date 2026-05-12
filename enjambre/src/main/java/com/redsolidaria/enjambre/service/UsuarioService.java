package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.model.Administrador;
import com.redsolidaria.enjambre.model.PersonaDiscapacitada;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.model.Voluntario;
import com.redsolidaria.enjambre.repository.AdministradorRepository;
import com.redsolidaria.enjambre.repository.PersonaDiscapacitadaRepository;
import com.redsolidaria.enjambre.repository.UsuarioRepository;
import com.redsolidaria.enjambre.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private VoluntarioRepository voluntarioRepository;
    
    @Autowired
    private PersonaDiscapacitadaRepository personaDiscapacitadaRepository;
    
    @Autowired
    private AdministradorRepository administradorRepository;

    // ========== MÉTODOS PARA ADMIN CONTROLLER ==========
    
    // Listar todos los usuarios
    public List<Usuario> listarTodosUsuarios() {
        return usuarioRepository.findAll();
    }
    
    // Listar voluntarios
    public List<Voluntario> listarVoluntarios() {
        return voluntarioRepository.findAll();
    }
    
    // Listar discapacitados
    public List<PersonaDiscapacitada> listarDiscapacitados() {
        return personaDiscapacitadaRepository.findAll();
    }
    
    // Listar administradores
    public List<Administrador> listarAdministradores() {
        return administradorRepository.findAll();
    }
    
    // Registrar administrador
    public void registrarAdministrador(String nombres, String apellidos, String email, String password) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        Administrador admin = new Administrador(nombres, apellidos, email, password);
        administradorRepository.save(admin);
        System.out.println("✅ Administrador registrado: " + email);
    }
    
    // Eliminar administrador
    public void eliminarAdministrador(Long id) throws Exception {
        Administrador admin = administradorRepository.findById(id)
            .orElseThrow(() -> new Exception("Administrador no encontrado"));
        
        // No permitir eliminar al admin principal
        if ("admin@redsolidaria.pe".equals(admin.getEmail())) {
            throw new Exception("❌ No se puede eliminar la cuenta principal de administrador");
        }
        administradorRepository.deleteById(id);
    }
    
    // Eliminar cualquier usuario por ID
    public void eliminarUsuario(Long id) throws Exception {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado"));
        
        // Verificar que no sea el admin principal
        if (usuario instanceof Administrador && "admin@redsolidaria.pe".equals(usuario.getEmail())) {
            throw new Exception("❌ No se puede eliminar la cuenta principal de administrador");
        }
        
        usuarioRepository.deleteById(id);
        System.out.println("✅ Usuario eliminado: " + usuario.getEmail());
    }
    
    // Buscar usuario por email
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    // ========== MÉTODOS PARA REGISTRO ==========
    
    // Registrar voluntario (verificado = false)
    public void registrarVoluntario(String nombres, String apellidos, String email, 
                                    String password, String codigo, String carrera) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        if (voluntarioRepository.existsByCodigo(codigo)) {
            throw new Exception("❌ El código de estudiante ya está registrado");
        }
        
        Voluntario voluntario = new Voluntario(nombres, apellidos, email, password, codigo, carrera);
        voluntario.setVerificado(false);
        
        voluntarioRepository.save(voluntario);
        System.out.println("✅ Voluntario guardado en BD: " + email);
    }
    
    // Registrar persona con discapacidad (verificado = false)
    public void registrarDiscapacitado(String nombres, String apellidos, String email,
                                       String password, String conadis, String tipoDiscapacidad,
                                       String telefono, String direccion) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        if (personaDiscapacitadaRepository.existsByConadis(conadis)) {
            throw new Exception("❌ El número CONADIS ya está registrado");
        }
        
        PersonaDiscapacitada persona = new PersonaDiscapacitada(nombres, apellidos, email, password, 
                                                                conadis, tipoDiscapacidad, telefono, direccion);
        persona.setVerificado(false);
        
        personaDiscapacitadaRepository.save(persona);
        System.out.println("✅ Persona con discapacidad guardada en BD: " + email);
    }
    
    // Marcar usuario como verificado (después del código)
    public void marcarComoVerificado(String email) throws Exception {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new Exception("Usuario no encontrado"));
        usuario.setVerificado(true);
        usuarioRepository.save(usuario);
        System.out.println("✅ Usuario verificado: " + email);
    }
}