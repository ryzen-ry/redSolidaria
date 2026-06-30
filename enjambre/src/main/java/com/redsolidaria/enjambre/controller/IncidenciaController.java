package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.model.HistorialAyuda;
import com.redsolidaria.enjambre.model.Incidencia;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.repository.HistorialAyudaRepository;
import com.redsolidaria.enjambre.repository.IncidenciaRepository;
import com.redsolidaria.enjambre.service.UsuarioService;
import com.redsolidaria.enjambre.service.ArchivoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class IncidenciaController {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private HistorialAyudaRepository historialAyudaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ArchivoService archivoService;

    // ========== DISCAPACITADO ==========

    @GetMapping("/discapacitado/incidencias")
    public String incidenciasDiscapacitado(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"DISCAPACITADO".equals(usuario.getRol())) {
            return "redirect:/login";
        }

        // Listar historiales del discapacitado
        List<HistorialAyuda> historiales = historialAyudaRepository
                .findBySolicitud_Discapacitado_IdOrderByFechaFinalizacionDesc(usuario.getId());
        
        // Listar incidencias reportadas por este discapacitado
        List<Incidencia> incidencias = incidenciaRepository
                .findByDenunciante_IdOrderByFechaCreacionDesc(usuario.getId());

        model.addAttribute("historiales", historiales);
        model.addAttribute("incidencias", incidencias);

        return "Users/Disca/incidenciasD";
    }

    @PostMapping("/discapacitado/incidencias/reportar")
    public String reportarIncidenciaDiscapacitado(@RequestParam Long historialId,
                                                  @RequestParam String titulo,
                                                  @RequestParam String descripcion,
                                                  @RequestParam(required = false) MultipartFile evidencia,
                                                  HttpSession session,
                                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"DISCAPACITADO".equals(usuario.getRol())) {
            return "redirect:/login";
        }

        try {
            HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                    .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado."));

            // El denunciado es el voluntario aceptado
            Usuario denunciado = historial.getSolicitud().getVoluntarioAceptado();
            if (denunciado == null) {
                throw new IllegalArgumentException("No hay un voluntario asociado a este historial para reportar.");
            }

            // Guardar evidencia usando ArchivoService si se ha adjuntado
            String evidenciaUrl = null;
            if (evidencia != null && !evidencia.isEmpty()) {
                evidenciaUrl = archivoService.guardarEvidencia(evidencia);
            }

            // Crear y guardar incidencia
            Incidencia incidencia = new Incidencia();
            incidencia.setHistorialAyuda(historial);
            incidencia.setDenunciante(usuario);
            incidencia.setDenunciado(denunciado);
            incidencia.setTitulo(titulo);
            incidencia.setDescripcion(descripcion);
            incidencia.setEvidenciaUrl(evidenciaUrl);
            incidencia.setEstado("PENDIENTE");
            incidencia.setFechaCreacion(LocalDateTime.now());

            incidenciaRepository.save(incidencia);

            redirectAttributes.addFlashAttribute("success", "✅ Incidencia reportada exitosamente. Se encuentra en estado PENDIENTE.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al reportar la incidencia: " + e.getMessage());
        }

        return "redirect:/discapacitado/incidencias";
    }

    // ========== VOLUNTARIO ==========

    @GetMapping("/voluntario/incidencias")
    public String incidenciasVoluntario(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"VOLUNTARIO".equals(usuario.getRol())) {
            return "redirect:/login";
        }

        // Listar historiales del voluntario
        List<HistorialAyuda> historiales = historialAyudaRepository
                .findBySolicitud_VoluntarioAceptado_IdOrderByFechaFinalizacionDesc(usuario.getId());

        // Listar incidencias reportadas por este voluntario
        List<Incidencia> incidencias = incidenciaRepository
                .findByDenunciante_IdOrderByFechaCreacionDesc(usuario.getId());

        model.addAttribute("historiales", historiales);
        model.addAttribute("incidencias", incidencias);

        return "Users/volun/incidenciasV";
    }

    @PostMapping("/voluntario/incidencias/reportar")
    public String reportarIncidenciaVoluntario(@RequestParam Long historialId,
                                               @RequestParam String titulo,
                                               @RequestParam String descripcion,
                                               @RequestParam(required = false) MultipartFile evidencia,
                                               HttpSession session,
                                               RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"VOLUNTARIO".equals(usuario.getRol())) {
            return "redirect:/login";
        }

        try {
            HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                    .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado."));

            // El denunciado es el discapacitado
            Usuario denunciado = historial.getSolicitud().getDiscapacitado();
            if (denunciado == null) {
                throw new IllegalArgumentException("No hay una persona con discapacidad asociada a este historial para reportar.");
            }

            // Guardar evidencia usando ArchivoService si se ha adjuntado
            String evidenciaUrl = null;
            if (evidencia != null && !evidencia.isEmpty()) {
                evidenciaUrl = archivoService.guardarEvidencia(evidencia);
            }

            // Crear y guardar incidencia
            Incidencia incidencia = new Incidencia();
            incidencia.setHistorialAyuda(historial);
            incidencia.setDenunciante(usuario);
            incidencia.setDenunciado(denunciado);
            incidencia.setTitulo(titulo);
            incidencia.setDescripcion(descripcion);
            incidencia.setEvidenciaUrl(evidenciaUrl);
            incidencia.setEstado("PENDIENTE");
            incidencia.setFechaCreacion(LocalDateTime.now());

            incidenciaRepository.save(incidencia);

            redirectAttributes.addFlashAttribute("success", "✅ Incidencia reportada exitosamente. Se encuentra en estado PENDIENTE.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al reportar la incidencia: " + e.getMessage());
        }

        return "redirect:/voluntario/incidencias";
    }
}
