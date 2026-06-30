// VoluntarioRepository.java
package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
    Optional<Voluntario> findByEmail(String email);
    Optional<Voluntario> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
