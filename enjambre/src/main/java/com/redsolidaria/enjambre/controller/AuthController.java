package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.PersonaDiscapacitadaDTO;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.VerificacionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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
    public String registroDiscapacitado(Model model) {
        model.addAttribute("personaDiscapacitadaDTO", new PersonaDiscapacitadaDTO());
        return "registroDis";
    }

    // ========== PROCESAR REGISTRO VOLUNTARIO ==========
    
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

    // ========== PROCESAR REGISTRO DISCAPACITADO (CON FOTOS) ==========
    
    @PostMapping("/registro/discapacitado")
    public String procesarRegistroDiscapacitado(@Valid PersonaDiscapacitadaDTO dto,
                                                BindingResult result,
                                                Model model) {
        
        // Validar que las contraseñas coinciden
        if (!dto.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error", "Las contraseñas no coinciden");
        }
        
        // ✅ CORREGIDO: usar dto.getPassword() en lugar de password
        if (dto.getPassword() != null && dto.getPassword().length() < 6) {
            result.rejectValue("password", "error", "La contraseña debe tener al menos 6 caracteres");
        }
        
        if (result.hasErrors()) {
            return "registroDis";
        }
        
        try {
            // Guardar las fotos en el servidor
            String dniDelanteraPath = guardarFoto(dto.getDniFotoDelantera(), "dni_delantera");
            String dniTraseraPath = guardarFoto(dto.getDniFotoTrasera(), "dni_trasera");
            String conadisPath = guardarFoto(dto.getConadisFoto(), "conadis");
            
            // Registrar al usuario con las rutas de las fotos
            usuarioService.registrarDiscapacitadoConFotos(
                dto.getNombres(), dto.getApellidos(), dto.getEmail(), 
                dto.getPassword(), dto.getConadis(), dto.getTipoDiscapacidad(),
                dto.getTelefono(), dto.getDireccion(),
                dniDelanteraPath, dniTraseraPath, conadisPath
            );
            
            // Enviar código de verificación
            verificacionService.enviarCodigo(dto.getEmail());
            
            model.addAttribute("email", dto.getEmail());
            model.addAttribute("nombreCompleto", dto.getNombres() + " " + dto.getApellidos());
            model.addAttribute("tipoUsuario", "discapacitado");
            
            return "verificar-codigo";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "registroDis";
        }
    }
    
    // Método auxiliar para guardar fotos
    private String guardarFoto(MultipartFile file, String prefijo) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // Crear directorio si no existe
        String uploadDir = "src/main/resources/static/uploads/documentos/";
        File directorio = new File(uploadDir);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
        
        // Generar nombre único para el archivo
        String nombreArchivo = prefijo + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + file.getOriginalFilename();
        String rutaCompleta = uploadDir + nombreArchivo;
        
        // Guardar el archivo
        Path path = Paths.get(rutaCompleta);
        Files.write(path, file.getBytes());
        
        // Retornar la ruta relativa para la web
        return "/uploads/documentos/" + nombreArchivo;
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
    
    // ========== PROCESAR LOGIN ==========

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email,
                                @RequestParam String password,
                                HttpSession session,
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
        
        // Guardar usuario en sesión
        session.setAttribute("usuario", usuario);
        
        // Redirigir según el rol
        String rol = usuario.getRol();
        
        switch (rol) {
            case "ADMIN":
                return "redirect:/admin/dashboard";
            case "VOLUNTARIO":
                return "redirect:/voluntario/inicio";
            case "DISCAPACITADO":
                return "redirect:/discapacitado/inicio";
            default:
                return "redirect:/";
        }
    }
    
    // ========== CERRAR SESIÓN ==========

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}