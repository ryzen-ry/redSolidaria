package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.Comentario;
import com.redsolidaria.enjambre.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    
    List<Comentario> findByPublicacionOrderByFechaCreacionAsc(Publicacion publicacion);
    
    long countByPublicacion(Publicacion publicacion);
    
    void deleteByUsuario_Id(Long usuarioId);
    
    void deleteByPublicacion_Usuario_Id(Long usuarioId);
}