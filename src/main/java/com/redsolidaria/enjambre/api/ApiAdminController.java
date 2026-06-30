package com.redsolidaria.enjambre.api;

import com.redsolidaria.enjambre.dto.AdminDTO;
import com.redsolidaria.enjambre.model.Administrador;
import com.redsolidaria.enjambre.model.PersonaDiscapacitada;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.model.Voluntario;
import com.redsolidaria.enjambre.model.HistorialActivacion;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.EmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

    // ========== DASHBOARD ==========
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios", usuarioService.listarTodosUsuarios().size());
        stats.put("totalVoluntarios", usuarioService.listarVoluntarios().size());
        stats.put("totalDiscapacitados", usuarioService.listarDiscapacitados().size());
        stats.put("totalAdministradores", usuarioService.listarAdministradores().size());
        stats.put("totalPendientes", usuarioService.listarUsuariosPendientes().size());
        return ResponseEntity.ok(stats);
    }

    // ========== GESTIÓN DE USUARIOS ==========
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> usuarios() {
        return ResponseEntity.ok(usuarioService.listarTodosUsuarios());
    }

    @GetMapping("/voluntarios")
    public ResponseEntity<List<Voluntario>> voluntarios() {
        return ResponseEntity.ok(usuarioService.listarVoluntarios());
    }

    @GetMapping("/discapacitados")
    public ResponseEntity<List<PersonaDiscapacitada>> discapacitados() {
        return ResponseEntity.ok(usuarioService.listarDiscapacitados());
    }

    // ========== GESTIÓN DE ADMINISTRADORES ==========
    @GetMapping("/administradores")
    public ResponseEntity<List<Administrador>> administradores() {
        return ResponseEntity.ok(usuarioService.listarAdministradores());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crearAdmin(@Valid @RequestBody AdminDTO adminDTO, BindingResult result) {
        if (!adminDTO.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error", "Las contraseñas no coinciden");
        }

        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(Map.of("error", "Errores de validación", "detalles", errors));
        }

        try {
            usuarioService.registrarAdministrador(
                    adminDTO.getNombres(),
                    adminDTO.getApellidos(),
                    adminDTO.getEmail(),
                    adminDTO.getPassword()
            );
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "✅ Administrador creado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear administrador: " + e.getMessage()));
        }
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminarAdmin(@PathVariable Long id) {
        try {
            usuarioService.eliminarAdministrador(id);
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "✅ Administrador eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/usuario/eliminar/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "✅ Usuario eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== ACTIVACIÓN DE USUARIOS PENDIENTES ==========
    @GetMapping("/usuarios/pendientes")
    public ResponseEntity<List<Usuario>> usuariosPendientes() {
        return ResponseEntity.ok(usuarioService.listarUsuariosPendientes());
    }

    @PostMapping("/usuarios/{id}/activar")
    public ResponseEntity<?> activarUsuario(@PathVariable Long id, HttpSession session) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "❌ Usuario no encontrado"));
            }

            Long adminId = null;
            Usuario adminSesion = (Usuario) session.getAttribute("usuario");
            if (adminSesion != null) {
                adminId = adminSesion.getId();
            }

            usuarioService.activarUsuario(id, adminId);
            emailService.enviarCorreoActivacion(usuario.getEmail());
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "✅ Cuenta activada exitosamente y notificación enviada por correo"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "❌ Error al activar la cuenta: " + e.getMessage()));
        }
    }

    @PostMapping("/usuarios/{id}/rechazar")
    public ResponseEntity<?> rechazarUsuario(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "❌ Usuario no encontrado"));
            }

            String email = usuario.getEmail();
            usuarioService.eliminarUsuario(id);
            emailService.enviarCorreoRechazo(email);
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "✅ Cuenta rechazada, eliminada de la base de datos y notificación enviada"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "❌ Error al rechazar la cuenta: " + e.getMessage()));
        }
    }

    @GetMapping("/historial-activaciones")
    public ResponseEntity<List<HistorialActivacion>> historialActivaciones() {
        return ResponseEntity.ok(usuarioService.listarHistorialActivaciones());
    }
}
