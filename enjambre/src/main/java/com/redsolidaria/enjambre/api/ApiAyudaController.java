package com.redsolidaria.enjambre.api;

import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.AyudaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ayuda")
public class ApiAyudaController {

    @Autowired
    private AyudaService ayudaService;

    @PostMapping("/ubicacion")
    public ResponseEntity<?> actualizarUbicacion(@RequestBody Map<String, Object> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado. Inicie sesión primero."));
        }

        try {
            if (payload.get("lat") == null || payload.get("lng") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los campos 'lat' y 'lng' son obligatorios"));
            }

            double lat = Double.parseDouble(payload.get("lat").toString());
            double lng = Double.parseDouble(payload.get("lng").toString());
            Double precision = payload.get("precisionMetros") != null 
                    ? Double.parseDouble(payload.get("precisionMetros").toString()) 
                    : null;

            ayudaService.actualizarUbicacion(usuario.getId(), lat, lng, precision);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al actualizar ubicación: " + e.getMessage()));
        }
    }

    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarAyuda(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"DISCAPACITADO".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado o rol incorrecto para solicitar ayuda."));
        }

        try {
            var solicitud = ayudaService.solicitarAyuda(usuario.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "solicitudId", solicitud.getId(),
                    "estado", solicitud.getEstado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al solicitar ayuda: " + e.getMessage()));
        }
    }

    @PostMapping("/responder")
    public ResponseEntity<?> responderAyuda(@RequestBody Map<String, Object> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"VOLUNTARIO".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado o rol incorrecto para responder a solicitudes."));
        }

        try {
            if (payload.get("solicitudId") == null || payload.get("decision") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los campos 'solicitudId' y 'decision' son obligatorios"));
            }

            Long solicitudId = Long.parseLong(payload.get("solicitudId").toString());
            String decision = payload.get("decision").toString();

            ayudaService.responderAyuda(solicitudId, usuario.getId(), decision);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al responder a la ayuda: " + e.getMessage()));
        }
    }

    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelarSolicitud(@RequestBody Map<String, Object> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"DISCAPACITADO".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado o rol incorrecto para cancelar solicitudes."));
        }

        try {
            if (payload.get("solicitudId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'solicitudId' es obligatorio"));
            }

            Long solicitudId = Long.parseLong(payload.get("solicitudId").toString());
            ayudaService.cancelarSolicitud(solicitudId, usuario.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al cancelar la solicitud: " + e.getMessage()));
        }
    }
}
