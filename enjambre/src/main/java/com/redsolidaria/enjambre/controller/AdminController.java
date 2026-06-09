package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.AdminDTO;
import com.redsolidaria.enjambre.model.Administrador;
import com.redsolidaria.enjambre.model.Usuario;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

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
}