package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.PersonaDiscapacitada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PersonaDiscapacitadaRepository extends JpaRepository<PersonaDiscapacitada, Long> {
    Optional<PersonaDiscapacitada> findByEmail(String email);
    Optional<PersonaDiscapacitada> findByConadis(String conadis);
    boolean existsByConadis(String conadis);
    boolean existsByCertificadoDiscapacidad(String certificadoDiscapacidad);
}