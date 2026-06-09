package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.SolicitudAyuda;
import com.redsolidaria.enjambre.model.SolicitudAyudaIntento;
import com.redsolidaria.enjambre.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudAyudaIntentoRepository extends JpaRepository<SolicitudAyudaIntento, Long> {

    List<SolicitudAyudaIntento> findBySolicitud_Id(Long solicitudId);

    Optional<SolicitudAyudaIntento> findBySolicitud_IdAndVoluntario_Id(Long solicitudId, Long voluntarioId);

    void deleteByVoluntario_Id(Long voluntarioId);

    void deleteBySolicitud_Discapacitado_Id(Long discapacitadoId);
}

