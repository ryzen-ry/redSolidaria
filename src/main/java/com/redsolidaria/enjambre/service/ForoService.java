package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.dto.ComentarioDTO;
import com.redsolidaria.enjambre.dto.PublicacionDTO;
import com.redsolidaria.enjambre.model.Comentario;
import com.redsolidaria.enjambre.model.Publicacion;
import com.redsolidaria.enjambre.model.Usuario;
import com.redsolidaria.enjambre.repository.ComentarioRepository;
import com.redsolidaria.enjambre.repository.PublicacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForoService {

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    // ========== PUBLICACIONES ==========

    public List<Publicacion> listarTodasPublicaciones() {
        return publicacionRepository.findAllOrderByFechaDesc();
    }

    public List<Publicacion> listarPublicacionesPorUsuario(Long usuarioId) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        return publicacionRepository.findByUsuario(usuario);
    }

    public List<Publicacion> listarPublicacionesPorCategoria(String categoria) {
        return publicacionRepository.findByCategoria(categoria);
    }

    public Publicacion crearPublicacion(PublicacionDTO dto, Long usuarioId) throws Exception {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new Exception("Usuario no encontrado");
        }

        Publicacion publicacion = new Publicacion(
            dto.getTitulo(),
            dto.getContenido(),
            dto.getCategoria(),
            usuario
        );

        return publicacionRepository.save(publicacion);
    }

    public Publicacion actualizarPublicacion(Long id, PublicacionDTO dto, Long usuarioId) throws Exception {
        Publicacion publicacion = publicacionRepository.findById(id)
            .orElseThrow(() -> new Exception("Publicación no encontrada"));

        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (!publicacion.getUsuario().getId().equals(usuarioId) && !"ADMIN".equals(usuario.getRol())) {
            throw new Exception("No tienes permiso para editar esta publicación");
        }

        publicacion.setTitulo(dto.getTitulo());
        publicacion.setContenido(dto.getContenido());
        publicacion.setCategoria(dto.getCategoria());

        return publicacionRepository.save(publicacion);
    }

    public void eliminarPublicacion(Long id, Long usuarioId) throws Exception {
        Publicacion publicacion = publicacionRepository.findById(id)
            .orElseThrow(() -> new Exception("Publicación no encontrada"));

        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (!publicacion.getUsuario().getId().equals(usuarioId) && !"ADMIN".equals(usuario.getRol())) {
            throw new Exception("No tienes permiso para eliminar esta publicación");
        }

        publicacionRepository.deleteById(id);
    }

    public void darLike(Long id) throws Exception {
        Publicacion publicacion = publicacionRepository.findById(id)
            .orElseThrow(() -> new Exception("Publicación no encontrada"));
        publicacion.setLikes(publicacion.getLikes() + 1);
        publicacionRepository.save(publicacion);
    }

    // ========== COMENTARIOS ==========

    public List<Comentario> listarComentariosPorPublicacion(Long publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId).orElse(null);
        if (publicacion == null) return List.of();
        return comentarioRepository.findByPublicacionOrderByFechaCreacionAsc(publicacion);
    }

    public Comentario crearComentario(Long publicacionId, ComentarioDTO dto, Long usuarioId) throws Exception {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
            .orElseThrow(() -> new Exception("Publicación no encontrada"));

        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new Exception("Usuario no encontrado");
        }

        Comentario comentario = new Comentario(dto.getContenido(), publicacion, usuario);
        return comentarioRepository.save(comentario);
    }

    public void eliminarComentario(Long id, Long usuarioId) throws Exception {
        Comentario comentario = comentarioRepository.findById(id)
            .orElseThrow(() -> new Exception("Comentario no encontrado"));

        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (!comentario.getUsuario().getId().equals(usuarioId) && !"ADMIN".equals(usuario.getRol())) {
            throw new Exception("No tienes permiso para eliminar este comentario");
        }

        comentarioRepository.deleteById(id);
    }

    public Comentario actualizarComentario(Long id, ComentarioDTO dto, Long usuarioId) throws Exception {
        Comentario comentario = comentarioRepository.findById(id)
            .orElseThrow(() -> new Exception("Comentario no encontrado"));

        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (!comentario.getUsuario().getId().equals(usuarioId) && !"ADMIN".equals(usuario.getRol())) {
            throw new Exception("No tienes permiso para editar este comentario");
        }

        comentario.setContenido(dto.getContenido());
        return comentarioRepository.save(comentario);
    }

    public long contarComentarios(Long publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId).orElse(null);
        if (publicacion == null) return 0;
        return comentarioRepository.countByPublicacion(publicacion);
    }

    // ✅ MÉTODO AGREGADO - contar comentarios por cada publicación
    public Map<Long, Long> contarComentariosPorPublicacion() {
        List<Publicacion> publicaciones = publicacionRepository.findAll();
        Map<Long, Long> mapa = new HashMap<>();
        for (Publicacion pub : publicaciones) {
            mapa.put(pub.getId(), comentarioRepository.countByPublicacion(pub));
        }
        return mapa;
    }
}