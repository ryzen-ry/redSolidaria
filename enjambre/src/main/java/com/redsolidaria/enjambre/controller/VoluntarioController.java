package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.repository.HistorialAyudaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/voluntario")
public class VoluntarioController {

    @Autowired
    private HistorialAyudaRepository historialAyudaRepository;

    @GetMapping("/inicio")
    public String inicio() {
        return "Users/volun/alertadeAyuda";
    }
    
    @GetMapping("/capacitacion")
    public String capacitacion() {
        return "Users/volun/capacitacion";
    }
    
    @GetMapping("/donaciones")
    public String donaciones() {
        return "Users/volun/donacionesVol";
    }
    
    @GetMapping("/historial")
    public String historial(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("historial", 
                historialAyudaRepository.findBySolicitud_VoluntarioAceptado_IdOrderByFechaFinalizacionDesc(usuario.getId()));
        }
        return "Users/volun/historialVolun";
    }
    
    @GetMapping("/foro")
    public String foro() {
        return "Users/volun/foro";
    }
    
    @GetMapping("/alertas")
    public String alertas() {
        return "Users/volun/alertadeAyuda";
    }
}