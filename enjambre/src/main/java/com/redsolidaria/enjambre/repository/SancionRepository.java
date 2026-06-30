package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.Sancion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SancionRepository extends JpaRepository<Sancion, Long> {
    List<Sancion> findByUsuario_Id(Long usuarioId);
    void deleteByUsuario_Id(Long usuarioId);
    
    @Modifying
    @Query("UPDATE Sancion s SET s.administrador = null WHERE s.administrador.id = :adminId")
    void desasociarAdministrador(@Param("adminId") Long adminId);
}
