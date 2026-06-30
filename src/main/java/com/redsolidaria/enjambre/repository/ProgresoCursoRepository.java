package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.ProgresoCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgresoCursoRepository extends JpaRepository<ProgresoCurso, Long> {
    Optional<ProgresoCurso> findByVoluntarioIdAndCursoId(Long voluntarioId, Long cursoId);
    List<ProgresoCurso> findByVoluntarioId(Long voluntarioId);
    void deleteByVoluntarioId(Long voluntarioId);
}
