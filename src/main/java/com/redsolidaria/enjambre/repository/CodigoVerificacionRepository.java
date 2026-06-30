package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {
    Optional<CodigoVerificacion> findByEmailAndCodigoAndUsadoFalse(String email, String codigo);
    void deleteByUsuario_Id(Long usuarioId);
}