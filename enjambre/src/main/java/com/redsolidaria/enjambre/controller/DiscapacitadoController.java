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
@RequestMapping("/discapacitado")
public class DiscapacitadoController {

    @Autowired
    private HistorialAyudaRepository historialAyudaRepository;

    // Página principal del discapacitado (donde redirige después del login)
    @GetMapping("/inicio")
    public String inicio() {
        return "Users/Disca/botonAyuda";
    }
    
    @GetMapping("/ayuda")
    public String ayuda() {
        return "Users/Disca/botonAyuda";
    }
    
    @GetMapping("/donaciones")
    public String donaciones() {
        return "Users/Disca/donacionesDis";
    }
    
    @GetMapping("/foro")
    public String foro() {
        return "Users/Disca/foroDis";
    }
    
    @GetMapping("/historial")
    public String historial(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("historial", 
                historialAyudaRepository.findBySolicitud_Discapacitado_IdOrderByFechaFinalizacionDesc(usuario.getId()));
        }
        return "Users/Disca/historialAyuda";
    }
}