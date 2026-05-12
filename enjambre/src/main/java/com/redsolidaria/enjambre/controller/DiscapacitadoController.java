package com.redsolidaria.enjambre.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/discapacitado")
public class DiscapacitadoController {

    // Página principal del discapacitado (donde redirige después del login)
    @GetMapping("/inicio")
    public String inicio() {
        return "/Users/Disca/botonAyuda";
    }
    
    @GetMapping("/ayuda")
    public String ayuda() {
        return "/Users/Disca/botonAyuda";
    }
    
    @GetMapping("/donaciones")
    public String donaciones() {
        return "/Users/Disca/donacionesDis";
    }
    
    @GetMapping("/foro")
    public String foro() {
        return "/Users/Disca/foroDis";
    }
    
    @GetMapping("/historial")
    public String historial() {
        return "/Users/Disca/historialAyuda";
    }
}