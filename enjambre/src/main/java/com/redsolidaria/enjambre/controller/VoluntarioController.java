package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.PreguntaDTO;
import com.redsolidaria.enjambre.dto.RespuestasTestDTO;
import com.redsolidaria.enjambre.model.Curso;
import com.redsolidaria.enjambre.model.Pregunta;
import com.redsolidaria.enjambre.model.ProgresoCurso;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.repository.HistorialAyudaRepository;
import com.redsolidaria.enjambre.service.CursoService;
import com.redsolidaria.enjambre.service.ProgresoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class VoluntarioController {

    @Autowired
    private HistorialAyudaRepository historialAyudaRepository;

    @Autowired
    private CursoService cursoService;

    @Autowired
    private ProgresoService progresoService;

    @GetMapping("/voluntario/inicio")
    public String inicio(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        long totalAyudados = 0;
        if (usuario != null && "VOLUNTARIO".equals(usuario.getRol())) {
            totalAyudados = historialAyudaRepository.countBySolicitud_VoluntarioAceptado_Id(usuario.getId());
        }
        model.addAttribute("totalAyudados", totalAyudados);
        return "Users/volun/alertadeAyuda";
    }

    @GetMapping("/voluntario/capacitacion")
    public String capacitacion(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || !"VOLUNTARIO".equals(usuario.getRol())) {
            return "redirect:/login";
        }

        // Obtener todos los cursos activos
        List<Curso> cursos = cursoService.listarCursosActivos();
        
        // Obtener progresos del voluntario
        List<ProgresoCurso> progresos = progresoService.obtenerProgresosDeVoluntario(usuario.getId());
        Map<Long, ProgresoCurso> progresosMap = progresos.stream()
            .collect(Collectors.toMap(p -> p.getCurso().getId(), p -> p));

        // Construir información de estado para Thymeleaf
        List<Map<String, Object>> cursosInfo = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (Curso curso : cursos) {
            Map<String, Object> info = new HashMap<>();
            info.put("curso", curso);
            
            ProgresoCurso progreso = progresosMap.get(curso.getId());
            if (progreso != null) {
                info.put("progreso", progreso);
                info.put("aprobado", progreso.isAprobado());
                
                boolean estaBloqueado = progreso.getFechaBloqueo() != null && progreso.getFechaBloqueo().isAfter(ahora);
                info.put("bloqueado", estaBloqueado);
                
                if (estaBloqueado) {
                    long minutosRestantes = Duration.between(ahora, progreso.getFechaBloqueo()).toMinutes() + 1;
                    info.put("minutosRestantes", minutosRestantes);
                }
                
                info.put("estado", progreso.getEstado());
            } else {
                info.put("aprobado", false);
                info.put("bloqueado", false);
                info.put("estado", "NO_INICIADO");
            }
            
            cursosInfo.add(info);
        }

        model.addAttribute("cursosInfo", cursosInfo);
        return "Users/volun/capacitacion";
    }

    @GetMapping("/voluntario/donaciones")
    public String donaciones() {
        return "Users/volun/donacionesVol";
    }

    @GetMapping("/voluntario/historial")
    public String historial(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("historial", 
                historialAyudaRepository.findBySolicitud_VoluntarioAceptado_IdOrderByFechaFinalizacionDesc(usuario.getId()));
        }
        return "Users/volun/historialVolun";
    }

    @GetMapping("/voluntario/foro")
    public String foro() {
        return "Users/volun/foro";
    }

    @GetMapping("/voluntario/alertas")
    public String alertas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        long totalAyudados = 0;
        if (usuario != null && "VOLUNTARIO".equals(usuario.getRol())) {
            totalAyudados = historialAyudaRepository.countBySolicitud_VoluntarioAceptado_Id(usuario.getId());
        }
        model.addAttribute("totalAyudados", totalAyudados);
        return "Users/volun/alertadeAyuda";
    }

    // ==========================================
    // ENDPOINTS AJAX / API PARA CAPACITACIÓN
    // ==========================================

    /**
     * Inicia un curso y registra que el voluntario ha abierto el video.
     */
    @PostMapping("/api/voluntario/curso/iniciar")
    @ResponseBody
    public ResponseEntity<?> iniciarCurso(@RequestBody Map<String, Long> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión no válida"));
        }

        Long cursoId = payload.get("cursoId");
        if (cursoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cursoId es requerido"));
        }

        try {
            ProgresoCurso progreso = progresoService.iniciarCursoVideo(usuario.getId(), cursoId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Curso iniciado con éxito",
                "estado", progreso.getEstado(),
                "videoCompletado", progreso.isVideoCompletado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Guarda el progreso del video (por ejemplo, cuando finaliza o supera el 90%).
     */
    @PostMapping("/api/voluntario/progreso/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarProgreso(@RequestBody Map<String, Object> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión no válida"));
        }

        Number cursoIdNum = (Number) payload.get("cursoId");
        Boolean completado = (Boolean) payload.get("completado");

        if (cursoIdNum == null || completado == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cursoId y completado son requeridos"));
        }

        try {
            ProgresoCurso progreso = progresoService.guardarProgresoVideo(usuario.getId(), cursoIdNum.longValue(), completado);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Progreso de video guardado",
                "videoCompletado", progreso.isVideoCompletado(),
                "estado", progreso.getEstado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verifica si el voluntario puede tomar el examen del curso.
     */
    @GetMapping("/api/voluntario/test/verificar")
    @ResponseBody
    public ResponseEntity<?> verificarPuedeHacerTest(@RequestParam Long cursoId, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión no válida"));
        }

        try {
            ProgresoCurso progreso = progresoService.obtenerOCrearProgreso(usuario.getId(), cursoId);
            boolean bloqueado = progresoService.estaBloqueado(progreso);

            if (bloqueado) {
                long minutosRestantes = Duration.between(LocalDateTime.now(), progreso.getFechaBloqueo()).toMinutes() + 1;
                return ResponseEntity.ok(Map.of(
                    "puedeHacerTest", false,
                    "mensaje", "El curso está bloqueado. Tiempo restante: " + minutosRestantes + " minutos."
                ));
            }

            if (!progreso.isVideoCompletado()) {
                return ResponseEntity.ok(Map.of(
                    "puedeHacerTest", false,
                    "mensaje", "Debe ver el video del curso antes de realizar el test."
                ));
            }

            if (progreso.isAprobado()) {
                return ResponseEntity.ok(Map.of(
                    "puedeHacerTest", false,
                    "mensaje", "Ya has aprobado este curso."
                ));
            }

            return ResponseEntity.ok(Map.of("puedeHacerTest", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene las preguntas del test asociadas a un curso (sin revelar las respuestas correctas).
     */
    @GetMapping("/api/voluntario/curso/{id}/preguntas")
    @ResponseBody
    public ResponseEntity<?> obtenerPreguntas(@PathVariable("id") Long cursoId, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión no válida"));
        }

        try {
            ProgresoCurso progreso = progresoService.obtenerOCrearProgreso(usuario.getId(), cursoId);
            if (progresoService.estaBloqueado(progreso)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "El curso está bloqueado."));
            }

            List<Pregunta> preguntas = cursoService.obtenerPreguntasDeCurso(cursoId);
            List<PreguntaDTO> dtos = preguntas.stream()
                .map(p -> new PreguntaDTO(p.getId(), p.getEnunciado(), p.getOpcionA(), p.getOpcionB(), p.getOpcionC(), p.getOpcionD()))
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Guarda y evalúa las respuestas enviadas para el test del curso.
     */
    @PostMapping("/api/voluntario/test/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarTest(@RequestBody Map<String, Object> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión no válida"));
        }

        Number cursoIdNum = (Number) payload.get("cursoId");
        Map<String, String> respuestasRaw = (Map<String, String>) payload.get("respuestas");

        if (cursoIdNum == null || respuestasRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cursoId y respuestas son requeridos"));
        }

        // Convertir mapa de String -> String a Long -> String
        Map<Long, String> respuestas = new HashMap<>();
        for (Map.Entry<String, String> entry : respuestasRaw.entrySet()) {
            try {
                respuestas.put(Long.parseLong(entry.getKey()), entry.getValue());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los IDs de las preguntas deben ser numéricos"));
            }
        }

        try {
            ProgresoCurso progreso = progresoService.guardarResultadoTest(usuario.getId(), cursoIdNum.longValue(), respuestas);
            
            boolean aprobado = progreso.isAprobado();
            String mensaje = aprobado ? "¡Aprobado! 🎉" : "No aprobaste. Puedes intentarlo en 1 hora";

            return ResponseEntity.ok(Map.of(
                "puntajeObtenido", progreso.getPuntajeObtenido(),
                "aprobado", aprobado,
                "intentos", progreso.getIntentos(),
                "mensaje", mensaje,
                "estado", progreso.getEstado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancela el curso y activa el bloqueo de 1 hora.
     */
    @PostMapping("/api/voluntario/curso/cancelar")
    @ResponseBody
    public ResponseEntity<?> cancelarCurso(@RequestBody Map<String, Long> payload, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión no válida"));
        }

        Long cursoId = payload.get("cursoId");
        if (cursoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cursoId es requerido"));
        }

        try {
            ProgresoCurso progreso = progresoService.cancelarCurso(usuario.getId(), cursoId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Has cancelado el curso. Puedes intentarlo en 1 hora",
                "estado", progreso.getEstado(),
                "bloqueado", true,
                "minutosRestantes", 60
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}