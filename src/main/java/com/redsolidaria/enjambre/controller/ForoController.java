package com.redsolidaria.enjambre.controller;

import com.redsolidaria.enjambre.dto.ComentarioDTO;
import com.redsolidaria.enjambre.dto.PublicacionDTO;
import com.redsolidaria.enjambre.model.Comentario;
import com.redsolidaria.enjambre.model.Publicacion;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.service.ForoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/foro")
public class ForoController {

    @Autowired
    private ForoService foroService;

    // ========== PUBLICACIONES ==========

    @GetMapping("/publicaciones")
    public ResponseEntity<?> listarPublicaciones(HttpSession session) {
        try {
            Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
            List<Publicacion> publicaciones = foroService.listarTodasPublicaciones();
            Map<Long, Long> comentariosCount = foroService.contarComentariosPorPublicacion();
            
            List<Map<String, Object>> resultado = publicaciones.stream().map(p -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("titulo", p.getTitulo());
                item.put("contenido", p.getContenido());
                item.put("categoria", p.getCategoria());
                item.put("likes", p.getLikes());
                item.put("fechaCreacion", p.getFechaCreacion());
                item.put("comentariosCount", comentariosCount.getOrDefault(p.getId(), 0L));
                item.put("usuario", Map.of(
                    "id", p.getUsuario().getId(),
                    "nombres", p.getUsuario().getNombres(),
                    "apellidos", p.getUsuario().getApellidos(),
                    "rol", p.getUsuario().getRol()
                ));
                item.put("miPublicacion", usuarioActual != null && p.getUsuario().getId().equals(usuarioActual.getId()));
                return item;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/publicaciones/usuario/{usuarioId}")
    public ResponseEntity<?> listarPublicacionesPorUsuario(@PathVariable Long usuarioId) {
        try {
            List<Publicacion> publicaciones = foroService.listarPublicacionesPorUsuario(usuarioId);
            return ResponseEntity.ok(publicaciones);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/publicaciones/categoria/{categoria}")
    public ResponseEntity<?> listarPublicacionesPorCategoria(@PathVariable String categoria) {
        try {
            List<Publicacion> publicaciones = foroService.listarPublicacionesPorCategoria(categoria);
            return ResponseEntity.ok(publicaciones);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/publicaciones")
    public ResponseEntity<?> crearPublicacion(@Valid @RequestBody PublicacionDTO dto, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            Publicacion publicacion = foroService.crearPublicacion(dto, usuario.getId());
            return ResponseEntity.ok(publicacion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/publicaciones/{id}")
    public ResponseEntity<?> actualizarPublicacion(@PathVariable Long id, @Valid @RequestBody PublicacionDTO dto, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            Publicacion publicacion = foroService.actualizarPublicacion(id, dto, usuario.getId());
            return ResponseEntity.ok(publicacion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/publicaciones/{id}")
    public ResponseEntity<?> eliminarPublicacion(@PathVariable Long id, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            foroService.eliminarPublicacion(id, usuario.getId());
            return ResponseEntity.ok(Map.of("mensaje", "Publicación eliminada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/publicaciones/{id}/like")
    public ResponseEntity<?> darLike(@PathVariable Long id) {
        try {
            foroService.darLike(id);
            return ResponseEntity.ok(Map.of("mensaje", "Like agregado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== COMENTARIOS ==========

    @GetMapping("/publicaciones/{id}/comentarios")
    public ResponseEntity<?> listarComentarios(@PathVariable Long id) {
        try {
            List<Comentario> comentarios = foroService.listarComentariosPorPublicacion(id);
            Map<String, Object> response = new HashMap<>();
            response.put("comentarios", comentarios);
            response.put("total", comentarios.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/publicaciones/{id}/comentarios")
    public ResponseEntity<?> crearComentario(@PathVariable Long id, @Valid @RequestBody ComentarioDTO dto, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            Comentario comentario = foroService.crearComentario(id, dto, usuario.getId());
            return ResponseEntity.ok(comentario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/comentarios/{id}")
    public ResponseEntity<?> eliminarComentario(@PathVariable Long id, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            foroService.eliminarComentario(id, usuario.getId());
            return ResponseEntity.ok(Map.of("mensaje", "Comentario eliminado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/comentarios/{id}")
    public ResponseEntity<?> actualizarComentario(@PathVariable Long id, @Valid @RequestBody ComentarioDTO dto, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            Comentario comentario = foroService.actualizarComentario(id, dto, usuario.getId());
            return ResponseEntity.ok(comentario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}