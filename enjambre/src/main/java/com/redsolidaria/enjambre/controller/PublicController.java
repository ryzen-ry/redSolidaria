package com.redsolidaria.enjambre.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/nosotros")
    public String nosotros() {
        return "nosotros";
    }

    @GetMapping("/capacitacion")
    public String capacitacion() {
        return "capacitacion";
    }

    @GetMapping("/donaciones")
    public String donaciones() {
        return "donaciones";
    }

    @GetMapping("/foro")
    public String foro() {
        return "foro";
    }
}