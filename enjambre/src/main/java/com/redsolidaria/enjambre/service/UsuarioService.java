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
    
    public List<Usuario> listarTodosUsuarios() {
        return usuarioRepository.findAll();
    }
    
    public List<Voluntario> listarVoluntarios() {
        return voluntarioRepository.findAll();
    }
    
    public List<PersonaDiscapacitada> listarDiscapacitados() {
        return personaDiscapacitadaRepository.findAll();
    }
    
    public List<Administrador> listarAdministradores() {
        return administradorRepository.findAll();
    }
    
    public void registrarAdministrador(String nombres, String apellidos, String email, String password) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        Administrador admin = new Administrador(nombres, apellidos, email, password);
        administradorRepository.save(admin);
        System.out.println("✅ Administrador registrado: " + email);
    }
    
    public void eliminarAdministrador(Long id) throws Exception {
        Administrador admin = administradorRepository.findById(id)
            .orElseThrow(() -> new Exception("Administrador no encontrado"));
        
        if ("admin@redsolidaria.pe".equals(admin.getEmail())) {
            throw new Exception("❌ No se puede eliminar la cuenta principal de administrador");
        }
        administradorRepository.deleteById(id);
    }
    
    public void eliminarUsuario(Long id) throws Exception {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado"));
        
        if (usuario instanceof Administrador && "admin@redsolidaria.pe".equals(usuario.getEmail())) {
            throw new Exception("❌ No se puede eliminar la cuenta principal de administrador");
        }
        
        usuarioRepository.deleteById(id);
        System.out.println("✅ Usuario eliminado: " + usuario.getEmail());
    }
    
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    // ========== MÉTODOS PARA REGISTRO ==========
    
    // Versión original (registro temporal con verificado = false)
    public void registrarVoluntario(String nombres, String apellidos, String email, 
                                    String password, String codigo, String carrera) throws Exception {
        registrarVoluntario(nombres, apellidos, email, password, codigo, carrera, false);
    }
    
    // ✅ NUEVO: Registrar voluntario con verificado personalizado
    public void registrarVoluntario(String nombres, String apellidos, String email, 
                                    String password, String codigo, String carrera, 
                                    boolean verificado) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        if (voluntarioRepository.existsByCodigo(codigo)) {
            throw new Exception("❌ El código de estudiante ya está registrado");
        }
        
        Voluntario voluntario = new Voluntario(nombres, apellidos, email, password, codigo, carrera);
        voluntario.setVerificado(verificado);
        
        voluntarioRepository.save(voluntario);
        System.out.println("✅ Voluntario guardado en BD: " + email + " | Verificado: " + verificado);
    }
    
    // Versión original (registro temporal con verificado = false)
    public void registrarDiscapacitado(String nombres, String apellidos, String email,
                                       String password, String conadis, String tipoDiscapacidad,
                                       String telefono, String direccion) throws Exception {
        registrarDiscapacitado(nombres, apellidos, email, password, conadis, tipoDiscapacidad,
                               telefono, direccion, false);
    }
    
    // ✅ NUEVO: Registrar discapacitado con verificado personalizado
    public void registrarDiscapacitado(String nombres, String apellidos, String email,
                                       String password, String conadis, String tipoDiscapacidad,
                                       String telefono, String direccion,
                                       boolean verificado) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        if (personaDiscapacitadaRepository.existsByConadis(conadis)) {
            throw new Exception("❌ El número CONADIS ya está registrado");
        }
        
        PersonaDiscapacitada persona = new PersonaDiscapacitada(nombres, apellidos, email, password, 
                                                                conadis, tipoDiscapacidad, telefono, direccion);
        persona.setVerificado(verificado);
        
        personaDiscapacitadaRepository.save(persona);
        System.out.println("✅ Persona con discapacidad guardada en BD: " + email + " | Verificado: " + verificado);
    }
    
    // ✅ NUEVO: Registrar discapacitado CON FOTOS con verificado personalizado
    public void registrarDiscapacitadoConFotos(String nombres, String apellidos, String email,
                                               String password, String conadis, String tipoDiscapacidad,
                                               String telefono, String direccion,
                                               String dniDelanteraUrl, String dniTraseraUrl, 
                                               String conadisFotoUrl) throws Exception {
        registrarDiscapacitadoConFotos(nombres, apellidos, email, password, conadis, tipoDiscapacidad,
                                       telefono, direccion, dniDelanteraUrl, dniTraseraUrl, conadisFotoUrl, false);
    }
    
    // ✅ NUEVO: Registrar discapacitado CON FOTOS con verificado personalizado
    public void registrarDiscapacitadoConFotos(String nombres, String apellidos, String email,
                                               String password, String conadis, String tipoDiscapacidad,
                                               String telefono, String direccion,
                                               String dniDelanteraUrl, String dniTraseraUrl, 
                                               String conadisFotoUrl,
                                               boolean verificado) throws Exception {
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new Exception("❌ El correo ya está registrado");
        }
        
        if (personaDiscapacitadaRepository.existsByConadis(conadis)) {
            throw new Exception("❌ El número CONADIS ya está registrado");
        }
        
        PersonaDiscapacitada persona = new PersonaDiscapacitada(nombres, apellidos, email, password, 
                                                                conadis, tipoDiscapacidad, telefono, direccion);
        persona.setDniDelanteraUrl(dniDelanteraUrl);
        persona.setDniTraseraUrl(dniTraseraUrl);
        persona.setConadisFotoUrl(conadisFotoUrl);
        persona.setVerificado(verificado);
        
        personaDiscapacitadaRepository.save(persona);
        System.out.println("✅ Persona con discapacidad CON FOTOS guardada en BD: " + email + " | Verificado: " + verificado);
        System.out.println("   📄 DNI Delantera: " + dniDelanteraUrl);
        System.out.println("   📄 DNI Trasera: " + dniTraseraUrl);
        System.out.println("   📄 CONADIS: " + conadisFotoUrl);
    }
    
    public void marcarComoVerificado(String email) throws Exception {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new Exception("Usuario no encontrado"));
        usuario.setVerificado(true);
        usuarioRepository.save(usuario);
        System.out.println("✅ Usuario verificado: " + email);
    }
    
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }
    
    public boolean existeCodigoVoluntario(String codigo) {
        return voluntarioRepository.existsByCodigo(codigo);
    }
    
    public boolean existeConadis(String conadis) {
        return personaDiscapacitadaRepository.existsByConadis(conadis);
    }
    // Buscar usuario por ID
     public Usuario buscarPorId(Long id) {
    return usuarioRepository.findById(id).orElse(null);
    }
}