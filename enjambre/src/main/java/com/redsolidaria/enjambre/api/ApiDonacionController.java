package com.redsolidaria.enjambre.api;

import com.redsolidaria.enjambre.dto.DonacionMonetariaDTO;
import com.redsolidaria.enjambre.dto.DonacionProductoDTO;
import com.redsolidaria.enjambre.model.DonacionMonetaria;
import com.redsolidaria.enjambre.model.DonacionProducto;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.DonacionService;
import com.redsolidaria.enjambre.service.EmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donaciones")
public class ApiDonacionController {

    @Autowired
    private DonacionService donacionService;

    @Autowired
    private EmailService emailService;

    // ==========================================
    // ENDPOINTS PARA USUARIOS (VOLUNTARIOS Y DISCAPACITADOS)
    // ==========================================

    @PostMapping("/monetaria/guardar-temporal")
    public ResponseEntity<?> guardarTemporal(@Valid @RequestBody DonacionMonetariaDTO dto, 
                                             BindingResult bindingResult, 
                                             HttpSession session) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida. Por favor, inicie sesión nuevamente."));
        }

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(Map.of("error", "Errores de validación", "detalles", errors));
        }

        try {
            Long donacionId = donacionService.guardarDonacionTemporal(dto, usuario.getId());
            return ResponseEntity.ok(Map.of("id", donacionId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la donación: " + e.getMessage()));
        }
    }

    @PostMapping("/monetaria/confirmar-codigo")
    public ResponseEntity<?> confirmarCodigo(@RequestBody Map<String, Object> payload, 
                                             HttpSession session) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida. Por favor, inicie sesión nuevamente."));
        }

        try {
            if (payload.get("id") == null || payload.get("codigoYape") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los campos 'id' y 'codigoYape' son obligatorios"));
            }
            
            Long id = Long.valueOf(payload.get("id").toString());
            String codigoYape = payload.get("codigoYape").toString();

            if (codigoYape.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El código de Yape no puede estar vacío"));
            }

            donacionService.confirmarCodigoYape(id, codigoYape);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar código Yape: " + e.getMessage()));
        }
    }

    @PostMapping("/productos/guardar")
    public ResponseEntity<?> guardarProducto(@Valid @RequestBody DonacionProductoDTO dto, 
                                             BindingResult bindingResult, 
                                             HttpSession session) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida. Por favor, inicie sesión nuevamente."));
        }

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(Map.of("error", "Errores de validación", "detalles", errors));
        }

        // Validaciones condicionales si elige recoger
        if ("recoger".equalsIgnoreCase(dto.getOpcionEntrega())) {
            if (dto.getDireccion() == null || dto.getDireccion().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La dirección es obligatoria para la opción de recoger."));
            }
            if (dto.getHorario() == null || dto.getHorario().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El horario es obligatorio para la opción de recoger."));
            }
        }

        try {
            donacionService.guardarDonacionProducto(dto, usuario.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la donación de producto: " + e.getMessage()));
        }
    }

    // ==========================================
    // ENDPOINTS PARA ADMINISTRADOR
    // ==========================================

    @GetMapping("/monetarias")
    public ResponseEntity<?> listarMonetarias(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado."));
        }
        return ResponseEntity.ok(donacionService.obtenerTodasMonetarias());
    }

    @GetMapping("/productos")
    public ResponseEntity<?> listarProductos(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado."));
        }
        return ResponseEntity.ok(donacionService.obtenerTodasProductos());
    }

    @PostMapping("/monetaria/confirmar/{id}")
    public ResponseEntity<?> adminConfirmarMonetaria(@PathVariable Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado."));
        }

        try {
            DonacionMonetaria donacion = donacionService.confirmarDonacionMonetaria(id);
            emailService.enviarConfirmacionMonetaria(donacion.getEmail(), donacion.getNombreCompleto(), donacion.getMonto());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al confirmar donación monetaria: " + e.getMessage()));
        }
    }

    @PostMapping("/monetaria/rechazar/{id}")
    public ResponseEntity<?> adminRechazarMonetaria(@PathVariable Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado."));
        }

        try {
            DonacionMonetaria donacion = donacionService.obtenerTodasMonetarias().stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación monetaria con ID: " + id));

            donacionService.rechazarDonacionMonetaria(id);
            emailService.enviarRechazoMonetaria(donacion.getEmail(), donacion.getNombreCompleto());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al rechazar donación monetaria: " + e.getMessage()));
        }
    }

    @PostMapping("/producto/confirmar/{id}")
    public ResponseEntity<?> adminConfirmarProducto(@PathVariable Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado."));
        }

        try {
            DonacionProducto donacion = donacionService.confirmarDonacionProducto(id);
            
            if ("recoger".equalsIgnoreCase(donacion.getOpcionEntrega())) {
                emailService.enviarConfirmacionProductoRecoger(
                        donacion.getEmail(), 
                        donacion.getNombreCompleto(), 
                        donacion.getTipoProducto(), 
                        donacion.getHorario()
                );
            } else {
                // "llevar"
                emailService.enviarConfirmacionProductoLlevar(
                        donacion.getEmail(), 
                        donacion.getNombreCompleto(), 
                        donacion.getTipoProducto(), 
                        "Av. Siempre Viva 123", 
                        "Lunes a Viernes 9am-6pm"
                );
            }

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al confirmar donación de producto: " + e.getMessage()));
        }
    }

    @PostMapping("/producto/rechazar/{id}")
    public ResponseEntity<?> adminRechazarProducto(@PathVariable Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado."));
        }

        try {
            DonacionProducto donacion = donacionService.obtenerTodasProductos().stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación de producto con ID: " + id));

            donacionService.rechazarDonacionProducto(id);
            emailService.enviarRechazoProducto(donacion.getEmail(), donacion.getNombreCompleto());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al rechazar donación de producto: " + e.getMessage()));
        }
    }
}
