package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.SolicitudAyuda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolicitudAyudaRepository extends JpaRepository<SolicitudAyuda, Long> {

    void deleteByDiscapacitado_Id(Long discapacitadoId);

    @Modifying
    @Query("UPDATE SolicitudAyuda s SET s.voluntarioAceptado = null WHERE s.voluntarioAceptado.id = :voluntarioId")
    void desasociarVoluntarioAceptado(@Param("voluntarioId") Long voluntarioId);

    // Bug #5: Verificar si el discapacitado ya tiene una solicitud PENDIENTE activa
    Optional<SolicitudAyuda> findTopByDiscapacitado_IdAndEstadoOrderByCreadaEnDesc(Long discapacitadoId, String estado);

    // Bug #6: Buscar sesión ACEPTADA donde el usuario participa (como discapacitado)
    Optional<SolicitudAyuda> findTopByDiscapacitado_IdAndEstadoOrderByAceptadaEnDesc(Long discapacitadoId, String estado);

    // Bug #6: Buscar sesión ACEPTADA donde el usuario participa (como voluntario)
    Optional<SolicitudAyuda> findTopByVoluntarioAceptado_IdAndEstadoOrderByAceptadaEnDesc(Long voluntarioId, String estado);
}
