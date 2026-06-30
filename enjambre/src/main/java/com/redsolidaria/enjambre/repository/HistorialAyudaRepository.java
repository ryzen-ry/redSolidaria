package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.HistorialAyuda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialAyudaRepository extends JpaRepository<HistorialAyuda, Long> {
    
    List<HistorialAyuda> findBySolicitud_Discapacitado_IdOrderByFechaFinalizacionDesc(Long discapacitadoId);
    
    List<HistorialAyuda> findBySolicitud_VoluntarioAceptado_IdOrderByFechaFinalizacionDesc(Long voluntarioId);

    long countBySolicitud_VoluntarioAceptado_Id(Long voluntarioId);

    void deleteBySolicitud_Discapacitado_Id(Long discapacitadoId);
    void deleteBySolicitud_VoluntarioAceptado_Id(Long voluntarioId);
}
