package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByCursoId(Long cursoId);
}
