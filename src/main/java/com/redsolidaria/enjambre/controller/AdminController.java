package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.AdminDTO;
import com.redsolidaria.enjambre.model.Administrador;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.model.HistorialAyuda;
import com.redsolidaria.enjambre.model.Incidencia;
import com.redsolidaria.enjambre.model.Sancion;
import com.redsolidaria.enjambre.repository.HistorialAyudaRepository;
import com.redsolidaria.enjambre.repository.IncidenciaRepository;
import com.redsolidaria.enjambre.repository.SancionRepository;
import com.redsolidaria.enjambre.repository.AdministradorRepository;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HistorialAyudaRepository historialAyudaRepository;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private SancionRepository sancionRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private com.redsolidaria.enjambre.ws.AyudaConnectionRegistry ayudaConnectionRegistry;

    // ========== DASHBOARD ==========
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsuarios", usuarioService.listarTodosUsuarios().size());
        model.addAttribute("totalVoluntarios", usuarioService.listarVoluntarios().size());
        model.addAttribute("totalDiscapacitados", usuarioService.listarDiscapacitados().size());
        model.addAttribute("totalAdministradores", usuarioService.listarAdministradores().size());
        model.addAttribute("totalPendientes", usuarioService.listarUsuariosPendientes().size());
        return "admin/dashboardAdm";
    }
    
    // ========== GESTIÓN DE USUARIOS ==========
    
    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodosUsuarios());
        return "admin/usuarios";
    }
    
    @GetMapping("/voluntarios")
    public String voluntarios(Model model) {
        model.addAttribute("voluntarios", usuarioService.listarVoluntarios());
        return "admin/voluntarios";
    }
    
    @GetMapping("/discapacitados")
    public String discapacitados(Model model) {
        model.addAttribute("discapacitados", usuarioService.listarDiscapacitados());
        return "admin/discapacitados";
    }
    
    // ========== GESTIÓN DE ADMINISTRADORES ==========
    
    @GetMapping("/administradores")
    public String administradores(Model model) {
        model.addAttribute("administradores", usuarioService.listarAdministradores());
        return "admin/administradores";
    }
    
    @GetMapping("/admin/nuevo")
    public String nuevoAdmin(Model model) {
        model.addAttribute("adminDTO", new AdminDTO());
        return "admin/admin-form";
    }
    
    @PostMapping("/admin/crear")
    public String crearAdmin(@Valid @ModelAttribute AdminDTO adminDTO,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        
        if (!adminDTO.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error", "Las contraseñas no coinciden");
            return "admin/admin-form";
        }
        
        if (result.hasErrors()) {
            return "admin/admin-form";
        }
        
        try {
            usuarioService.registrarAdministrador(
                adminDTO.getNombres(),
                adminDTO.getApellidos(),
                adminDTO.getEmail(),
                adminDTO.getPassword()
            );
            redirectAttributes.addFlashAttribute("success", "✅ Administrador creado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/administradores";
    }
    
    @GetMapping("/admin/eliminar/{id}")
    public String eliminarAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarAdministrador(id);
            redirectAttributes.addFlashAttribute("success", "✅ Administrador eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/administradores";
    }
    
    @GetMapping("/foro")
    public String foro() {
        return "admin/foro";
    }
    
    @GetMapping("/usuario/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("success", "✅ Usuario eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ========== ACTIVACIÓN DE USUARIOS PENDIENTES ==========

    @GetMapping("/activacion")
    public String activacion(Model model) {
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("historialActivaciones", usuarioService.listarHistorialActivaciones());
        return "admin/activacion-usuarios";
    }

    @PostMapping("/usuarios/{id}/activar")
    public String activarUsuario(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "❌ Usuario no encontrado");
                return "redirect:/admin/activacion";
            }
            Long adminId = null;
            Usuario adminSesion = (Usuario) session.getAttribute("usuario");
            if (adminSesion != null) {
                adminId = adminSesion.getId();
            }
            usuarioService.activarUsuario(id, adminId);
            emailService.enviarCorreoActivacion(usuario.getEmail());
            redirectAttributes.addFlashAttribute("success", "✅ Cuenta activada exitosamente y notificación enviada por correo");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al activar la cuenta: " + e.getMessage());
        }
        return "redirect:/admin/activacion";
    }

    @PostMapping("/usuarios/{id}/rechazar")
    public String rechazarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "❌ Usuario no encontrado");
                return "redirect:/admin/activacion";
            }
            String email = usuario.getEmail();
            usuarioService.eliminarUsuario(id); // Elimina físicamente de la base de datos
            emailService.enviarCorreoRechazo(email);
            redirectAttributes.addFlashAttribute("success", "✅ Cuenta rechazada, eliminada de la base de datos y notificación enviada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al rechazar la cuenta: " + e.getMessage());
        }
        return "redirect:/admin/activacion";
    }

    // ========== GESTIÓN DE INCIDENCIAS ==========
    
    @GetMapping("/incidencias")
    public String incidencias(Model model, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || !"ADMIN".equals(admin.getRol())) {
            return "redirect:/login";
        }

        List<Incidencia> incidencias = incidenciaRepository.findAllByOrderByFechaCreacionDesc();
        // Transición automática a PROCESANDO al ser visualizadas por el admin
        for (Incidencia inc : incidencias) {
            if ("PENDIENTE".equals(inc.getEstado())) {
                inc.setEstado("PROCESANDO");
                incidenciaRepository.save(inc);
            }
        }
        model.addAttribute("incidencias", incidencias);

        Map<Long, List<Sancion>> sancionesPorUsuario = new HashMap<>();
        for (Incidencia h : incidencias) {
            if (h.getDenunciado() != null) {
                Long volId = h.getDenunciado().getId();
                sancionesPorUsuario.putIfAbsent(volId, sancionRepository.findByUsuario_Id(volId));
            }
            if (h.getDenunciante() != null) {
                Long discId = h.getDenunciante().getId();
                sancionesPorUsuario.putIfAbsent(discId, sancionRepository.findByUsuario_Id(discId));
            }
        }
        model.addAttribute("sancionesPorUsuario", sancionesPorUsuario);

        return "admin/incidencias";
    }

    @PostMapping("/incidencias/sancionar")
    public String sancionar(@RequestParam Long historialId,
                            @RequestParam Long reportedUserId,
                            @RequestParam String tipoSancion,
                            @RequestParam(required = false) String motivo,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || !"ADMIN".equals(admin.getRol())) {
            return "redirect:/login";
        }

        try {
            Usuario reportedUser = usuarioService.buscarPorId(reportedUserId);
            if (reportedUser == null) {
                redirectAttributes.addFlashAttribute("error", "❌ Usuario reportado no encontrado");
                return "redirect:/admin/incidencias";
            }

            HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));

            Administrador administrador = administradorRepository.findById(admin.getId())
                .orElseThrow(() -> new IllegalArgumentException("Administrador no encontrado"));

            Sancion sancion = new Sancion(reportedUser, historial, tipoSancion, motivo, administrador);
            sancionRepository.save(sancion);

            // Marcar las incidencias correspondientes como RESUELTO y notificar por correo al denunciante
            List<Incidencia> incidenciasAsociadas = incidenciaRepository.findByHistorialAyuda_IdAndDenunciado_Id(historialId, reportedUserId);
            String resolucionDetalles = "";
            if ("AVISO_1".equals(tipoSancion)) {
                resolucionDetalles = "Se ha aplicado un Primer Aviso de Advertencia al usuario reportado.";
            } else if ("AVISO_2".equals(tipoSancion)) {
                resolucionDetalles = "Se ha aplicado un Segundo Aviso de Advertencia al usuario reportado.";
            } else if ("BLOQUEO".equals(tipoSancion)) {
                resolucionDetalles = "Se ha inhabilitado permanentemente la cuenta del usuario reportado por el siguiente motivo: " + motivo;
            }

            for (Incidencia inc : incidenciasAsociadas) {
                inc.setEstado("RESUELTO");
                incidenciaRepository.save(inc);
                if (inc.getDenunciante() != null) {
                    emailService.enviarResolucionIncidencia(
                        inc.getDenunciante().getEmail(),
                        inc.getDenunciante().getNombreCompleto(),
                        inc.getDenunciado().getNombreCompleto(),
                        resolucionDetalles
                    );

                    // Notificación WebSocket al denunciante
                    try {
                        java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                        wsPayload.put("type", "INCIDENCIA_RESUELTA");
                        wsPayload.put("incidenciaId", inc.getId());
                        wsPayload.put("resolucion", "La resolución para tu reporte contra " + inc.getDenunciado().getNombreCompleto() + " es: " + resolucionDetalles);
                        ayudaConnectionRegistry.sendToUser(inc.getDenunciante().getId(), wsPayload);
                    } catch (Exception wsEx) {
                        System.err.println("[WS] Error enviando notificación de resolución: " + wsEx.getMessage());
                    }
                }
            }

            // Notificación WebSocket al denunciado (sancionado)
            try {
                java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                wsPayload.put("type", "SANCION_RECIBIDA");
                wsPayload.put("tipoSancion", tipoSancion);
                wsPayload.put("motivo", "Has recibido una sanción (" + tipoSancion + ") por: " + (motivo != null && !motivo.isEmpty() ? motivo : resolucionDetalles));
                ayudaConnectionRegistry.sendToUser(reportedUserId, wsPayload);
            } catch (Exception wsEx) {
                System.err.println("[WS] Error enviando notificación de sanción: " + wsEx.getMessage());
            }

            boolean isVoluntario = "VOLUNTARIO".equals(reportedUser.getRol());

            if ("AVISO_1".equals(tipoSancion)) {
                emailService.enviarPrimerAvisoIncidencia(reportedUser.getEmail(), isVoluntario);
                redirectAttributes.addFlashAttribute("success", "✅ Primer aviso registrado, incidencia resuelta y notificaciones enviadas");
            } else if ("AVISO_2".equals(tipoSancion)) {
                emailService.enviarSegundoAvisoIncidencia(reportedUser.getEmail(), isVoluntario);
                redirectAttributes.addFlashAttribute("success", "✅ Segundo aviso registrado, incidencia resuelta y notificaciones enviadas");
            } else if ("BLOQUEO".equals(tipoSancion)) {
                reportedUser.setEstado("BLOQUEADO");
                usuarioService.guardarUsuario(reportedUser);
                emailService.enviarBloqueoCuentaIncidencia(reportedUser.getEmail(), isVoluntario, motivo);
                redirectAttributes.addFlashAttribute("success", "🚫 Cuenta bloqueada permanentemente, incidencia resuelta y notificaciones enviadas");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al aplicar sanción: " + e.getMessage());
        }

        return "redirect:/admin/incidencias";
    }
}