package com.redsolidaria.enjambre.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/voluntario")
public class VoluntarioController {

    @GetMapping("/inicio")
    public String inicio() {
        return "/Users/volun/capacitacion";
    }
    
    @GetMapping("/capacitacion")
    public String capacitacion() {
        return "/Users/volun/capacitacion";
    }
    
    @GetMapping("/donaciones")
    public String donaciones() {
        return "/Users/volun/donacionesVol";
    }
    
    @GetMapping("/historial")
    public String historial() {
        return "/Users/volun/historialVolun";
    }
    
    @GetMapping("/foro")
    public String foro() {
        return "/Users/volun/foro";
    }
}