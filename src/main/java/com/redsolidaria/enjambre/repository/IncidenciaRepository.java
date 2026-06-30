package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findAllByOrderByFechaCreacionDesc();
    List<Incidencia> findByHistorialAyuda_IdAndDenunciado_Id(Long historialAyudaId, Long denunciadoId);
    List<Incidencia> findByDenunciante_IdOrderByFechaCreacionDesc(Long denuncianteId);
}
