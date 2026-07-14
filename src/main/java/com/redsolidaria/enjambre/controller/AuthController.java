package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.PersonaDiscapacitadaDTO;
import com.redsolidaria.enjambre.model.RegistroTemporalDiscapacitado;
import com.redsolidaria.enjambre.model.RegistroTemporalVoluntario;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.VerificacionService;
import com.redsolidaria.enjambre.service.UsuarioBloqueadoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private UsuarioBloqueadoService usuarioBloqueadoService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        if (nombres == null || nombres.trim().isEmpty()) {
            model.addAttribute("error", "❌ Los nombres son obligatorios");
            return "registroVol";
        }

        if (apellidos == null || apellidos.trim().isEmpty()) {
            model.addAttribute("error", "❌ Los apellidos son obligatorios");
            return "registroVol";
        }

        if (codigo == null || codigo.trim().isEmpty()) {
            model.addAttribute("error", "❌ El código de estudiante es obligatorio");
            return "registroVol";
        }

        if (carrera == null || carrera.trim().isEmpty()) {
            model.addAttribute("error", "❌ Debes seleccionar tu carrera");
            return "registroVol";
        }

        String mensajeBloqueo = usuarioBloqueadoService.mensajeBloqueoVoluntario(email, codigo);
        if (mensajeBloqueo != null) {
            model.addAttribute("error", mensajeBloqueo);
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
            
            // ✅ Inicializar contador de seguridad: 3 intentos máximos + token único por sesión
            session.setAttribute("verificacionIntentos", 3);
            String verifyToken = UUID.randomUUID().toString();
            session.setAttribute("verificacionToken", verifyToken);
            
            model.addAttribute("email", email);
            model.addAttribute("nombreCompleto", nombres + " " + apellidos);
            model.addAttribute("tipoUsuario", "voluntario");
            model.addAttribute("intentosRestantes", 3);
            model.addAttribute("verifyToken", verifyToken);
            
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

        String mensajeBloqueo = usuarioBloqueadoService.mensajeBloqueoDiscapacitado(
                dto.getEmail(), dto.getConadis(), dto.getCertificadoDiscapacidad());
        if (mensajeBloqueo != null) {
            result.reject("bloqueado", mensajeBloqueo);
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
        
        // Validar documentos requeridos
        if (dto.getDniFotoDelantera() == null || dto.getDniFotoDelantera().isEmpty()) {
            result.rejectValue("dniFotoDelantera", "error", "❌ Debes subir la foto del DNI (parte delantera)");
        }
        if (dto.getDniFotoTrasera() == null || dto.getDniFotoTrasera().isEmpty()) {
            result.rejectValue("dniFotoTrasera", "error", "❌ Debes subir la foto del DNI (parte trasera)");
        }
        if (dto.getConadisFoto() == null || dto.getConadisFoto().isEmpty()) {
            result.rejectValue("conadisFoto", "error", "❌ Debes subir la foto del Carnet CONADIS");
        }

        if (result.hasErrors()) {
            model.addAttribute("personaDiscapacitadaDTO", dto);
            if (result.getGlobalError() != null) {
                model.addAttribute("error", result.getGlobalError().getDefaultMessage());
            } else if (result.getFieldErrorCount() > 0) {
                model.addAttribute("error", "❌ Por favor corrige los errores señalados en el formulario");
            }
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
            
            // ✅ Inicializar contador de seguridad: 3 intentos máximos + token único por sesión
            session.setAttribute("verificacionIntentos", 3);
            String verifyTokenDis = UUID.randomUUID().toString();
            session.setAttribute("verificacionToken", verifyTokenDis);
            
            model.addAttribute("email", dto.getEmail());
            model.addAttribute("nombreCompleto", dto.getNombres() + " " + dto.getApellidos());
            model.addAttribute("tipoUsuario", "discapacitado");
            model.addAttribute("intentosRestantes", 3);
            model.addAttribute("verifyToken", verifyTokenDis);
            
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
                                   @RequestParam(required = false, defaultValue = "false") String tiempoExpirado,
                                   HttpSession session,
                                   Model model) {

        // ── Seguridad: verificar si el tiempo expiró (enviado desde el frontend) ──
        if ("true".equals(tiempoExpirado)) {
            // Limpiar sesión de registro temporal completa
            session.removeAttribute("registroTempVol");
            session.removeAttribute("registroTempDis");
            session.removeAttribute("verificacionIntentos");
            session.removeAttribute("verificacionToken");
            return "redirect:/registro?expired=1";
        }

        // ── Seguridad: verificar intentos restantes ─────────────────────────────
        Integer intentosRestantes = (Integer) session.getAttribute("verificacionIntentos");
        if (intentosRestantes == null) intentosRestantes = 3; // fallback

        if (intentosRestantes <= 0) {
            // Sin intentos disponibles: limpiar sesión y redirigir
            session.removeAttribute("registroTempVol");
            session.removeAttribute("registroTempDis");
            session.removeAttribute("verificacionIntentos");
            return "redirect:/registro?expired=1";
        }

        boolean valido = verificacionService.verificarCodigo(email, codigo);
        
        if (valido) {
            // ✅ Limpiar contador de intentos
            session.removeAttribute("verificacionIntentos");

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
                        model.addAttribute("intentosRestantes", intentosRestantes);
                        model.addAttribute("verifyToken", session.getAttribute("verificacionToken"));
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
                        model.addAttribute("intentosRestantes", intentosRestantes);
                        model.addAttribute("verifyToken", session.getAttribute("verificacionToken"));
                        return "verificar-codigo";
                    }
                }
            }
            
            model.addAttribute("mensaje", "✅ ¡Código verificado con éxito! Tu cuenta está en revisión por el administrador. Te notificaremos por correo una vez activa.");
            return "login";
        } else {
            // ── Descontar un intento ────────────────────────────────────────────
            int nuevosIntentos = intentosRestantes - 1;
            session.setAttribute("verificacionIntentos", nuevosIntentos);

            if (nuevosIntentos <= 0) {
                // Agotó los 3 intentos: limpiar sesión completa y redirigir al registro
                session.removeAttribute("registroTempVol");
                session.removeAttribute("registroTempDis");
                session.removeAttribute("verificacionIntentos");
                session.removeAttribute("verificacionToken");
                return "redirect:/registro?expired=1";
            }

            // Quedan intentos: mostrar error con contador actualizado
            String msgError = nuevosIntentos == 1
                ? "❌ Código inválido. ¡Cuidado! Te queda solo 1 intento."
                : "❌ Código inválido. Te quedan " + nuevosIntentos + " intentos.";

            model.addAttribute("error", msgError);
            model.addAttribute("email", email);
            model.addAttribute("tipoUsuario", tipoUsuario);
            model.addAttribute("intentosRestantes", nuevosIntentos);
            model.addAttribute("verifyToken", session.getAttribute("verificacionToken"));
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
        
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
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

        if ("BLOQUEADO".equals(usuario.getEstado())) {
            model.addAttribute("error", "❌ Tu cuenta ha sido bloqueada permanentemente por conductas inadecuadas.");
            return "login";
        }
        
        // Guardar usuario en sesión
        session.setAttribute("usuario", usuario);
        // Guardar también atributos primitivos para el handshake WS.
        // Evita problemas cuando el objeto completo no puede resolverse en ese contexto.
        session.setAttribute("usuarioId", usuario.getId());
        session.setAttribute("usuarioRol", usuario.getRol());
        
        // Autenticar programáticamente en Spring Security
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
            Authentication authenticated = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authenticated);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
        } catch (Exception e) {
            System.err.println("❌ Fallo en la autenticación programática de Spring Security: " + e.getMessage());
        }
        
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