package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.model.Curso;
import com.redsolidaria.enjambre.model.Pregunta;
import com.redsolidaria.enjambre.repository.CursoRepository;
import com.redsolidaria.enjambre.repository.PreguntaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;

    public List<Curso> listarCursosActivos() {
        return cursoRepository.findByActivoTrue();
    }

    public List<Pregunta> obtenerPreguntasDeCurso(Long cursoId) {
        return preguntaRepository.findByCursoId(cursoId);
    }

    public Curso obtenerPorId(Long id) {
        return cursoRepository.findById(id).orElse(null);
    }
}
