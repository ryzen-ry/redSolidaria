package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.PersonaDiscapacitadaDTO;
import com.redsolidaria.enjambre.model.RegistroTemporalDiscapacitado;
import com.redsolidaria.enjambre.model.RegistroTemporalVoluntario;
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

    // ========== PROCESAR REGISTRO VOLUNTARIO (GUARDA EN SESIÓN) ==========
    
    @PostMapping("/registro/voluntario")
    public String procesarRegistroVoluntario(@RequestParam String email,
                                              @RequestParam String nombres,
                                              @RequestParam String apellidos,
                                              @RequestParam String codigo,
                                              @RequestParam String carrera,
                                              @RequestParam String password,
                                              @RequestParam(required = false) String confirmPassword,
                                              @RequestParam(required = false) MultipartFile fotoPerfil,
                                              @RequestParam(required = false) MultipartFile certificadoLaboral, // ✅ NUEVO CAMPO
                                              HttpSession session,
                                              Model model) {
        
        String lowerEmail = email.toLowerCase();
        if (!lowerEmail.matches("^u\\d{8}@utp\\.edu\\.pe$")) {
            model.addAttribute("error", "Debes usar tu correo institucional con el formato u12345678@utp.edu.pe");
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
        
        // Validar que el email no esté ya registrado en BD
        if (usuarioService.existeEmail(email)) {
            model.addAttribute("error", "❌ El correo ya está registrado");
            return "registroVol";
        }
        
        // Validar que el código no esté ya registrado
        if (usuarioService.existeCodigoVoluntario(codigo)) {
            model.addAttribute("error", "❌ El código de estudiante ya está registrado");
            return "registroVol";
        }
        
        // ✅ Validar que se haya subido el Certificado Único Laboral
        if (certificadoLaboral == null || certificadoLaboral.isEmpty()) {
            model.addAttribute("error", "❌ Debes subir tu Certificado Único Laboral (PDF)");
            return "registroVol";
        }
        
        // ✅ Validar que el archivo sea PDF
        if (!certificadoLaboral.getContentType().equals("application/pdf")) {
            model.addAttribute("error", "❌ El Certificado Único Laboral debe ser un archivo PDF válido");
            return "registroVol";
        }
        
        // ✅ Validar tamaño máximo (ejemplo: 5MB)
        if (certificadoLaboral.getSize() > 5 * 1024 * 1024) {
            model.addAttribute("error", "❌ El archivo PDF no debe superar los 5MB");
            return "registroVol";
        }
        
        try {
            // ✅ Guardar foto de perfil si se subió
            String fotoPerfilPath = null;
            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                fotoPerfilPath = guardarFoto(fotoPerfil, "foto_perfil");
            }
            
            // ✅ Guardar Certificado Único Laboral
            String certificadoPath = guardarFoto(certificadoLaboral, "certificado_laboral");
            
            // ✅ Guardar datos temporalmente en sesión (NO en BD)
            RegistroTemporalVoluntario temp = new RegistroTemporalVoluntario();
            temp.setNombres(nombres);
            temp.setApellidos(apellidos);
            temp.setEmail(email);
            temp.setCodigo(codigo);
            temp.setCarrera(carrera);
            temp.setPassword(password);
            temp.setFotoPerfilPath(fotoPerfilPath);        // ✅ AGREGAR ESTE CAMPO
            temp.setCertificadoLaboralPath(certificadoPath); // ✅ AGREGAR ESTE CAMPO
            
            session.setAttribute("registroTempVol", temp);
            
            // ✅ Enviar código de verificación
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

    // ========== PROCESAR REGISTRO DISCAPACITADO (GUARDA EN SESIÓN) ==========
    
    @PostMapping("/registro/discapacitado")
    public String procesarRegistroDiscapacitado(@Valid PersonaDiscapacitadaDTO dto,
                                                BindingResult result,
                                                HttpSession session,
                                                Model model) {
        
        // Validar que las contraseñas coinciden
        if (!dto.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error", "Las contraseñas no coinciden");
        }
        
        if (dto.getPassword() != null && dto.getPassword().length() < 6) {
            result.rejectValue("password", "error", "La contraseña debe tener al menos 6 caracteres");
        }
        
        // Validar dominio permitido para correos de personas con discapacidad
        String lowerEmailDto = dto.getEmail() != null ? dto.getEmail().toLowerCase() : "";
        if (!(lowerEmailDto.endsWith("@gmail.com") || lowerEmailDto.endsWith("@hotmail.com"))) {
            result.rejectValue("email", "error", "Debes usar un correo @gmail.com o @hotmail.com");
        }

        // Validar que el email no esté ya registrado en BD
        if (usuarioService.existeEmail(dto.getEmail())) {
            result.rejectValue("email", "error", "❌ El correo ya está registrado");
        }
        
        // Validar que el DNI no esté ya registrado
        if (usuarioService.existeConadis(dto.getConadis())) {
            result.rejectValue("conadis", "error", "❌ El número de DNI ya está registrado");
        }
        
        // Validar que el certificado de discapacidad no esté ya registrado
        if (usuarioService.existeCertificadoDiscapacidad(dto.getCertificadoDiscapacidad())) {
            result.rejectValue("certificadoDiscapacidad", "error", "❌ El certificado de discapacidad ya está registrado");
        }
        
        if (result.hasErrors()) {
            return "registroDis";
        }
        
        try {
            // Guardar las fotos temporalmente en el servidor
            String dniDelanteraPath = guardarFoto(dto.getDniFotoDelantera(), "dni_delantera");
            String dniTraseraPath = guardarFoto(dto.getDniFotoTrasera(), "dni_trasera");
            String conadisPath = guardarFoto(dto.getConadisFoto(), "conadis");
            
            // ✅ Guardar datos temporalmente en sesión (NO en BD)
            RegistroTemporalDiscapacitado temp = new RegistroTemporalDiscapacitado();
            temp.setNombres(dto.getNombres());
            temp.setApellidos(dto.getApellidos());
            temp.setEmail(dto.getEmail());
            temp.setConadis(dto.getConadis());
            temp.setCertificadoDiscapacidad(dto.getCertificadoDiscapacidad());
            temp.setTipoDiscapacidad(dto.getTipoDiscapacidad());
            temp.setTelefono(dto.getTelefono());
            temp.setDireccion(dto.getDireccion());
            temp.setPassword(dto.getPassword());
            temp.setDniDelanteraPath(dniDelanteraPath);
            temp.setDniTraseraPath(dniTraseraPath);
            temp.setConadisFotoPath(conadisPath);
            
            session.setAttribute("registroTempDis", temp);
            
            // ✅ Enviar código de verificación
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
    
    // Método auxiliar para guardar fotos/documentos
    private String guardarFoto(MultipartFile file, String prefijo) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // Crear directorio si no existe
        String uploadDir = "uploads/documentos/";
        File directorio = new File(uploadDir);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
        
        // Obtener la extensión del archivo
        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // Generar nombre único para el archivo
        String nombreArchivo = prefijo + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        String rutaCompleta = uploadDir + nombreArchivo;
        
        // Guardar el archivo
        Path path = Paths.get(rutaCompleta);
        Files.write(path, file.getBytes());
        
        // Retornar la ruta relativa para la web
        return "/uploads/documentos/" + nombreArchivo;
    }

    // ========== VERIFICAR CÓDIGO (AQUÍ SE GUARDA EN BD CON verificado = TRUE) ==========
    
    @PostMapping("/verificar-codigo")
    public String verificarCodigo(@RequestParam String email,
                                   @RequestParam String codigo,
                                   @RequestParam String tipoUsuario,
                                   HttpSession session,
                                   Model model) {
        
        boolean valido = verificacionService.verificarCodigo(email, codigo);
        
        if (valido) {
            // ✅ Recuperar datos temporales de la sesión y guardar en BD con verificado = TRUE
            if ("voluntario".equals(tipoUsuario)) {
                RegistroTemporalVoluntario temp = (RegistroTemporalVoluntario) session.getAttribute("registroTempVol");
                
                if (temp != null && temp.getEmail().equals(email)) {
                    try {
                        // ✅ verificado = TRUE (pasando también la ruta del certificado)
                        usuarioService.registrarVoluntario(
                            temp.getNombres(), temp.getApellidos(), temp.getEmail(),
                            temp.getPassword(), temp.getCodigo(), temp.getCarrera(),
                            temp.getFotoPerfilPath(),      // ✅ NUEVO PARÁMETRO
                            temp.getCertificadoLaboralPath(), // ✅ NUEVO PARÁMETRO
                            true
                        );
                        session.removeAttribute("registroTempVol");
                    } catch (Exception e) {
                        model.addAttribute("error", "Error al guardar usuario: " + e.getMessage());
                        return "verificar-codigo";
                    }
                }
            } else if ("discapacitado".equals(tipoUsuario)) {
                RegistroTemporalDiscapacitado temp = (RegistroTemporalDiscapacitado) session.getAttribute("registroTempDis");
                
                if (temp != null && temp.getEmail().equals(email)) {
                    try {
                        // ✅ verificado = TRUE
                        usuarioService.registrarDiscapacitadoConFotos(
                            temp.getNombres(), temp.getApellidos(), temp.getEmail(),
                            temp.getPassword(), temp.getConadis(), temp.getCertificadoDiscapacidad(),
                            temp.getTipoDiscapacidad(), temp.getTelefono(), temp.getDireccion(),
                            temp.getDniDelanteraPath(), temp.getDniTraseraPath(), temp.getConadisFotoPath(),
                            true
                        );
                        session.removeAttribute("registroTempDis");
                    } catch (Exception e) {
                        model.addAttribute("error", "Error al guardar usuario: " + e.getMessage());
                        return "verificar-codigo";
                    }
                }
            }
            
            model.addAttribute("mensaje", "✅ ¡Código verificado con éxito! Tu cuenta está en revisión por el administrador. Te notificaremos por correo una vez activa.");
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
        
        if ("PENDIENTE".equals(usuario.getEstado())) {
            model.addAttribute("error", "❌ Cuenta en revisión por administrador");
            return "login";
        }
        
        // Guardar usuario en sesión
        session.setAttribute("usuario", usuario);
        // Guardar también atributos primitivos para el handshake WS.
        // Evita problemas cuando el objeto completo no puede resolverse en ese contexto.
        session.setAttribute("usuarioId", usuario.getId());
        session.setAttribute("usuarioRol", usuario.getRol());
        
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
        return "redirect:/";
    }
}