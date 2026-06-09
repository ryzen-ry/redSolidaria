package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.Publicacion;
import com.redsolidaria.enjambre.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    
    List<Publicacion> findByUsuario(Usuario usuario);
    
    void deleteByUsuario_Id(Long usuarioId);
    
    List<Publicacion> findByCategoria(String categoria);
    
    @Query("SELECT p FROM Publicacion p ORDER BY p.fechaCreacion DESC")
    List<Publicacion> findAllOrderByFechaDesc();
    
    @Query("SELECT p FROM Publicacion p WHERE p.usuario.rol = :rol ORDER BY p.fechaCreacion DESC")
    List<Publicacion> findByRolUsuario(@Param("rol") String rol);
}