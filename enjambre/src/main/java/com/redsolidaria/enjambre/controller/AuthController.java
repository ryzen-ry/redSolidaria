package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.VerificacionService;
import jakarta.servlet.http.HttpSession;  // ← IMPORTANTE: Agregar esta importación
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VerificacionService verificacionService;

    // ========== PÁGINAS DE LOGIN/REGISTRO ==========

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @GetMapping("/registro/voluntario")
    public String registroVoluntario() {
        return "registroVol";
    }

    @GetMapping("/registro/discapacitado")
    public String registroDiscapacitado() {
        return "registroDis";
    }

    // ========== PROCESAR REGISTROS ==========
    
    @PostMapping("/registro/voluntario")
    public String procesarRegistroVoluntario(@RequestParam String email,
                                              @RequestParam String nombres,
                                              @RequestParam String apellidos,
                                              @RequestParam String codigo,
                                              @RequestParam String carrera,
                                              @RequestParam String password,
                                              @RequestParam(required = false) String confirmPassword,
                                              Model model) {
        
        if (!email.endsWith("@utp.edu.pe")) {
            model.addAttribute("error", "Debes usar tu correo institucional @utp.edu.pe");
            return "registroVol";
        }
        
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "registroVol";
        }
        
        if (password.length() < 6) {
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            return "registroVol";
        }
        
        try {
            usuarioService.registrarVoluntario(nombres, apellidos, email, password, codigo, carrera);
            verificacionService.enviarCodigo(email);
            
            model.addAttribute("email", email);
            model.addAttribute("nombreCompleto", nombres + " " + apellidos);
            model.addAttribute("tipoUsuario", "voluntario");
            
            return "verificar-codigo";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "registroVol";
        }
    }

    @PostMapping("/registro/discapacitado")
    public String procesarRegistroDiscapacitado(@RequestParam String email,
                                                 @RequestParam String nombres,
                                                 @RequestParam String apellidos,
                                                 @RequestParam String conadis,
                                                 @RequestParam String tipoDiscapacidad,
                                                 @RequestParam String telefono,
                                                 @RequestParam String direccion,
                                                 @RequestParam String password,
                                                 @RequestParam(required = false) String confirmPassword,
                                                 Model model) {
        
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "registroDis";
        }
        
        if (password.length() < 6) {
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            return "registroDis";
        }
        
        try {
            usuarioService.registrarDiscapacitado(nombres, apellidos, email, password, conadis, tipoDiscapacidad, telefono, direccion);
            verificacionService.enviarCodigo(email);
            
            model.addAttribute("email", email);
            model.addAttribute("nombreCompleto", nombres + " " + apellidos);
            model.addAttribute("tipoUsuario", "discapacitado");
            
            return "verificar-codigo";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "registroDis";
        }
    }

    // ========== VERIFICAR CÓDIGO ==========
    
    @PostMapping("/verificar-codigo")
    public String verificarCodigo(@RequestParam String email,
                                   @RequestParam String codigo,
                                   @RequestParam String tipoUsuario,
                                   Model model) {
        
        boolean valido = verificacionService.verificarCodigo(email, codigo);
        
        if (valido) {
            model.addAttribute("mensaje", "✅ ¡Cuenta verificada con éxito! Ya puedes iniciar sesión.");
            return "login";
        } else {
            model.addAttribute("error", "❌ Código inválido o expirado. Vuelve a intentarlo.");
            model.addAttribute("email", email);
            model.addAttribute("tipoUsuario", tipoUsuario);
            return "verificar-codigo";
        }
    }
    
    // ========== PROCESAR LOGIN (MODIFICADO) ==========

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email,
                                @RequestParam String password,
                                HttpSession session,  // ← NUEVO: Agregar sesión
                                Model model) {
        
        Usuario usuario = usuarioService.buscarPorEmail(email);
        
        if (usuario == null) {
            model.addAttribute("error", "❌ El correo no está registrado");
            return "login";
        }
        
        if (!usuario.getPassword().equals(password)) {
            model.addAttribute("error", "❌ Contraseña incorrecta");
            return "login";
        }
        
        if (!usuario.isVerificado()) {
            model.addAttribute("error", "❌ Debes verificar tu cuenta. Revisa tu correo.");
            return "login";
        }
        
        // ✅ NUEVO: Guardar usuario en sesión
        session.setAttribute("usuario", usuario);
        
        // Redirigir según el rol (NUEVAS RUTAS)
        String rol = usuario.getRol();
        
        switch (rol) {
            case "ADMIN":
                return "redirect:/admin/dashboard";
            case "VOLUNTARIO":
                return "redirect:/voluntario/inicio";  // ← CAMBIADO
            case "DISCAPACITADO":
                return "redirect:/discapacitado/inicio";  // ← CAMBIADO
            default:
                return "redirect:/";
        }
    }
    // ========== CERRAR SESIÓN ==========

@GetMapping("/logout")
public String cerrarSesion(HttpSession session) {
    session.invalidate();  // Eliminar la sesión
    return "redirect:/login?logout=true";
}
}