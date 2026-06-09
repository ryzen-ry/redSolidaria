package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.HistorialActivacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialActivacionRepository extends JpaRepository<HistorialActivacion, Long> {

    @Query("SELECT h FROM HistorialActivacion h " +
           "LEFT JOIN FETCH h.usuario " +
           "LEFT JOIN FETCH h.administrador " +
           "ORDER BY h.fechaActivacion DESC")
    List<HistorialActivacion> findAllWithUsuarioAndAdministrador();

    void deleteByUsuario_Id(Long usuarioId);

    void deleteByAdministrador_Id(Long adminId);
}
