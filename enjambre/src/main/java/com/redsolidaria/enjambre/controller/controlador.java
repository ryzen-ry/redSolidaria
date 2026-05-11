package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.service.VerificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class controlador {

    @Autowired
    private VerificacionService verificacionService;

    // Páginas estáticas
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
    @GetMapping("/login")
    public String login() {
        return "login";
    }


    @GetMapping("/foro")
    public String foro() {
        return "foro";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @GetMapping("/registro/voluntario")
    public String registroVoluntario() {
        return "registroVol";
    }

    @GetMapping("/registro/discapacitado")
    public String registroDiscapacitado() {
        return "registroDis";
    }

    // ========== PROCESAR REGISTROS ==========
    
    @PostMapping("/registro/voluntario")
    public String procesarRegistroVoluntario(@RequestParam String email,
                                              @RequestParam String nombres,
                                              @RequestParam String apellidos,
                                              @RequestParam String codigo,
                                              @RequestParam String carrera,
                                              @RequestParam String password,
                                              Model model) {
        
        // Validar que el correo sea UTP
        if (!email.endsWith("@utp.edu.pe")) {
            model.addAttribute("error", "Debes usar tu correo institucional @utp.edu.pe");
            return "registroVol";
        }
        
        // Enviar código de verificación
        verificacionService.enviarCodigo(email);
        
        // Guardar datos temporalmente (en sesión o BD temporal)
        // Por ahora los pasamos a la siguiente página
        model.addAttribute("email", email);
        model.addAttribute("nombreCompleto", nombres + " " + apellidos);
        model.addAttribute("tipoUsuario", "voluntario");
        
        return "verificar-codigo";
    }

    @PostMapping("/registro/discapacitado")
    public String procesarRegistroDiscapacitado(@RequestParam String email,
                                                 @RequestParam String nombres,
                                                 @RequestParam String apellidos,
                                                 @RequestParam String conadis,
                                                 @RequestParam String tipoDiscapacidad,
                                                 @RequestParam String telefono,
                                                 @RequestParam String direccion,
                                                 @RequestParam String password,
                                                 Model model) {
        
        verificacionService.enviarCodigo(email);
        
        model.addAttribute("email", email);
        model.addAttribute("nombreCompleto", nombres + " " + apellidos);
        model.addAttribute("tipoUsuario", "discapacitado");
        
        return "verificar-codigo";
    }

    // ========== VERIFICAR CÓDIGO ==========
    
    @PostMapping("/verificar-codigo")
    public String verificarCodigo(@RequestParam String email,
                                   @RequestParam String codigo,
                                   @RequestParam String tipoUsuario,
                                   Model model) {
        
        boolean valido = verificacionService.verificarCodigo(email, codigo);
        
        if (valido) {
            model.addAttribute("mensaje", "✅ ¡Cuenta verificada con éxito! Ya puedes iniciar sesión.");
            return "login";
        } else {
            model.addAttribute("error", "❌ Código inválido o expirado. Vuelve a intentarlo.");
            model.addAttribute("email", email);
            model.addAttribute("tipoUsuario", tipoUsuario);
            return "verificar-codigo";
        }
    }
}