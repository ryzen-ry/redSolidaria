package com.redsolidaria.enjambre.api;

import com.redsolidaria.enjambre.dto.PersonaDiscapacitadaDTO;
import com.redsolidaria.enjambre.model.RegistroTemporalDiscapacitado;
import com.redsolidaria.enjambre.model.RegistroTemporalVoluntario;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.VerificacionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VerificacionService verificacionService;

    // ========== INICIAR SESIÓN ==========
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ Correo y contraseña son obligatorios"));
        }

        Usuario usuario = usuarioService.buscarPorEmail(email);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "❌ El correo no está registrado"));
        }

        if (!usuario.getPassword().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "❌ Contraseña incorrecta"));
        }

        if (!usuario.isVerificado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "❌ Debes verificar tu cuenta. Revisa tu correo."));
        }

        if ("PENDIENTE".equals(usuario.getEstado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "❌ Cuenta en revisión por el administrador"));
        }

        // Guardar usuario en la sesión
        session.setAttribute("usuario", usuario);
        session.setAttribute("usuarioId", usuario.getId());
        session.setAttribute("usuarioRol", usuario.getRol());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("usuario", Map.of(
                "id", usuario.getId(),
                "nombres", usuario.getNombres(),
                "apellidos", usuario.getApellidos(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol(),
                "estado", usuario.getEstado()
        ));

        return ResponseEntity.ok(response);
    }

    // ========== CERRAR SESIÓN ==========
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Sesión cerrada correctamente"));
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutGet(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Sesión cerrada correctamente"));
    }

    // ========== PROCESAR REGISTRO VOLUNTARIO ==========
    @PostMapping(value = "/registro/voluntario", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrarVoluntario(
            @RequestParam("email") String email,
            @RequestParam("nombres") String nombres,
            @RequestParam("apellidos") String apellidos,
            @RequestParam("codigo") String codigo,
            @RequestParam("carrera") String carrera,
            @RequestParam("password") String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            @RequestParam("certificadoLaboral") MultipartFile certificadoLaboral,
            HttpSession session) {

        String lowerEmail = email.toLowerCase();
        if (!lowerEmail.matches("^u\\d{8}@utp\\.edu\\.pe$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes usar tu correo institucional con el formato u12345678@utp.edu.pe"));
        }

        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }

        if (usuarioService.existeEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ El correo ya está registrado"));
        }

        if (usuarioService.existeCodigoVoluntario(codigo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ El código de estudiante ya está registrado"));
        }

        if (certificadoLaboral == null || certificadoLaboral.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ Debes subir tu Certificado Único Laboral (PDF)"));
        }

        if (!"application/pdf".equals(certificadoLaboral.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ El Certificado Único Laboral debe ser un archivo PDF válido"));
        }

        if (certificadoLaboral.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ El archivo PDF no debe superar los 5MB"));
        }

        try {
            String fotoPerfilPath = null;
            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                fotoPerfilPath = guardarFoto(fotoPerfil, "foto_perfil");
            }

            String certificadoPath = guardarFoto(certificadoLaboral, "certificado_laboral");

            RegistroTemporalVoluntario temp = new RegistroTemporalVoluntario();
            temp.setNombres(nombres);
            temp.setApellidos(apellidos);
            temp.setEmail(email);
            temp.setCodigo(codigo);
            temp.setCarrera(carrera);
            temp.setPassword(password);
            temp.setFotoPerfilPath(fotoPerfilPath);
            temp.setCertificadoLaboralPath(certificadoPath);

            session.setAttribute("registroTempVol", temp);

            verificacionService.enviarCodigo(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Código de verificación enviado",
                    "email", email,
                    "tipoUsuario", "voluntario"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar voluntario: " + e.getMessage()));
        }
    }

    // ========== PROCESAR REGISTRO DISCAPACITADO ==========
    @PostMapping(value = "/registro/discapacitado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrarDiscapacitado(
            @ModelAttribute @Valid PersonaDiscapacitadaDTO dto,
            BindingResult result,
            HttpSession session) {

        if (!dto.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error", "Las contraseñas no coinciden");
        }

        if (dto.getPassword() != null && dto.getPassword().length() < 6) {
            result.rejectValue("password", "error", "La contraseña debe tener al menos 6 caracteres");
        }

        String lowerEmailDto = dto.getEmail() != null ? dto.getEmail().toLowerCase() : "";

        // Solo permitir gmail.com o hotmail.com para personas con discapacidad
        if (!(lowerEmailDto.endsWith("@gmail.com") || lowerEmailDto.endsWith("@hotmail.com"))) {
            result.rejectValue("email", "error", "Debes usar un correo @gmail.com o @hotmail.com");
        }

        if (usuarioService.existeEmail(dto.getEmail())) {
            result.rejectValue("email", "error", "❌ El correo ya está registrado");
        }

        if (usuarioService.existeConadis(dto.getConadis())) {
            result.rejectValue("conadis", "error", "❌ El número de DNI ya está registrado");
        }

        if (usuarioService.existeCertificadoDiscapacidad(dto.getCertificadoDiscapacidad())) {
            result.rejectValue("certificadoDiscapacidad", "error", "❌ El certificado de discapacidad ya está registrado");
        }

        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(Map.of("error", "Errores de validación", "detalles", errors));
        }

        try {
            String dniDelanteraPath = guardarFoto(dto.getDniFotoDelantera(), "dni_delantera");
            String dniTraseraPath = guardarFoto(dto.getDniFotoTrasera(), "dni_trasera");
            String conadisPath = guardarFoto(dto.getConadisFoto(), "conadis");

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

            verificacionService.enviarCodigo(dto.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Código de verificación enviado",
                    "email", dto.getEmail(),
                    "tipoUsuario", "discapacitado"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar discapacitado: " + e.getMessage()));
        }
    }

    // ========== VERIFICAR CÓDIGO ==========
    // ========== CHECK EMAIL (AJAX) ==========
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requerido"));
        }
        boolean exists = usuarioService.existeEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<?> verificarCodigo(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String codigo = payload.get("codigo");
        String tipoUsuario = payload.get("tipoUsuario");

        if (email == null || codigo == null || tipoUsuario == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ email, codigo y tipoUsuario son obligatorios"));
        }

        boolean valido = verificacionService.verificarCodigo(email, codigo);

        if (valido) {
            if ("voluntario".equals(tipoUsuario)) {
                RegistroTemporalVoluntario temp = (RegistroTemporalVoluntario) session.getAttribute("registroTempVol");

                if (temp != null && temp.getEmail().equals(email)) {
                    try {
                        usuarioService.registrarVoluntario(
                                temp.getNombres(), temp.getApellidos(), temp.getEmail(),
                                temp.getPassword(), temp.getCodigo(), temp.getCarrera(),
                                temp.getFotoPerfilPath(),
                                temp.getCertificadoLaboralPath(),
                                true
                        );
                        session.removeAttribute("registroTempVol");
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Error al guardar voluntario: " + e.getMessage()));
                    }
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "❌ No se encontraron datos de registro temporal para el correo proporcionado"));
                }
            } else if ("discapacitado".equals(tipoUsuario)) {
                RegistroTemporalDiscapacitado temp = (RegistroTemporalDiscapacitado) session.getAttribute("registroTempDis");

                if (temp != null && temp.getEmail().equals(email)) {
                    try {
                        usuarioService.registrarDiscapacitadoConFotos(
                                temp.getNombres(), temp.getApellidos(), temp.getEmail(),
                                temp.getPassword(), temp.getConadis(), temp.getCertificadoDiscapacidad(),
                                temp.getTipoDiscapacidad(), temp.getTelefono(), temp.getDireccion(),
                                temp.getDniDelanteraPath(), temp.getDniTraseraPath(), temp.getConadisFotoPath(),
                                true
                        );
                        session.removeAttribute("registroTempDis");
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Error al guardar discapacitado: " + e.getMessage()));
                    }
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "❌ No se encontraron datos de registro temporal para el correo proporcionado"));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "❌ Tipo de usuario no válido"));
            }

            return ResponseEntity.ok(Map.of("success", true, "mensaje", "✅ ¡Código verificado con éxito! Cuenta en revisión por administrador."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ Código inválido o expirado. Vuelve a intentarlo."));
        }
    }

    private String guardarFoto(MultipartFile file, String prefijo) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String uploadDir = "uploads/documentos/";
        File directorio = new File(uploadDir);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String nombreArchivo = prefijo + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        String rutaCompleta = uploadDir + nombreArchivo;

        Path path = Paths.get(rutaCompleta);
        Files.write(path, file.getBytes());

        return "/uploads/documentos/" + nombreArchivo;
    }
}
