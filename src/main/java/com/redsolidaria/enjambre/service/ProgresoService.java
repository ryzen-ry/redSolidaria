package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.model.Curso;
import com.redsolidaria.enjambre.model.Pregunta;
import com.redsolidaria.enjambre.model.ProgresoCurso;
import com.redsolidaria.enjambre.model.Voluntario;
import com.redsolidaria.enjambre.repository.CursoRepository;
import com.redsolidaria.enjambre.repository.PreguntaRepository;
import com.redsolidaria.enjambre.repository.ProgresoCursoRepository;
import com.redsolidaria.enjambre.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProgresoService {

    @Autowired
    private ProgresoCursoRepository progresoCursoRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;

    /**
     * Obtiene o crea el progreso de un voluntario para un curso específico.
     */
    @Transactional
    public ProgresoCurso obtenerOCrearProgreso(Long voluntarioId, Long cursoId) {
        Optional<ProgresoCurso> progresoOpt = progresoCursoRepository.findByVoluntarioIdAndCursoId(voluntarioId, cursoId);
        if (progresoOpt.isPresent()) {
            return progresoOpt.get();
        }

        Voluntario voluntario = voluntarioRepository.findById(voluntarioId)
            .orElseThrow(() -> new IllegalArgumentException("Voluntario no encontrado con ID: " + voluntarioId));
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado con ID: " + cursoId));

        ProgresoCurso nuevoProgreso = new ProgresoCurso(voluntario, curso);
        return progresoCursoRepository.save(nuevoProgreso);
    }

    /**
     * Verifica si el progreso del curso está bloqueado actualmente por tiempo (1 hora).
     */
    public boolean estaBloqueado(ProgresoCurso progreso) {
        if (progreso == null || progreso.getFechaBloqueo() == null) {
            return false;
        }
        return progreso.getFechaBloqueo().isAfter(LocalDateTime.now());
    }

    /**
     * Registra el inicio de la visualización del video.
     */
    @Transactional
    public ProgresoCurso iniciarCursoVideo(Long voluntarioId, Long cursoId) {
        ProgresoCurso progreso = obtenerOCrearProgreso(voluntarioId, cursoId);

        if (estaBloqueado(progreso)) {
            throw new IllegalStateException("El curso está bloqueado. Inténtelo más tarde.");
        }

        if (progreso.isAprobado()) {
            throw new IllegalStateException("Este curso ya ha sido aprobado.");
        }

        if (progreso.getFechaInicioVideo() == null) {
            progreso.setFechaInicioVideo(LocalDateTime.now());
            progreso.setEstado("EN_PROGRESO");
            progreso = progresoCursoRepository.save(progreso);
        }
        return progreso;
    }

    /**
     * Guarda el progreso de visualización del video (si ya terminó / al 90%).
     */
    @Transactional
    public ProgresoCurso guardarProgresoVideo(Long voluntarioId, Long cursoId, boolean completado) {
        ProgresoCurso progreso = obtenerOCrearProgreso(voluntarioId, cursoId);

        if (estaBloqueado(progreso)) {
            throw new IllegalStateException("El curso está bloqueado.");
        }

        if (progreso.isAprobado()) {
            return progreso;
        }

        if (completado) {
            progreso.setVideoCompletado(true);
            if (progreso.getFechaFinVideo() == null) {
                progreso.setFechaFinVideo(LocalDateTime.now());
            }
            if ("EN_PROGRESO".equals(progreso.getEstado())) {
                progreso.setEstado("COMPLETADO");
            }
        }
        return progresoCursoRepository.save(progreso);
    }

    /**
     * Cancela el curso de manera voluntaria, lo que genera un bloqueo de 1 hora.
     */
    @Transactional
    public ProgresoCurso cancelarCurso(Long voluntarioId, Long cursoId) {
        ProgresoCurso progreso = obtenerOCrearProgreso(voluntarioId, cursoId);

        if (estaBloqueado(progreso)) {
            throw new IllegalStateException("El curso ya está bloqueado.");
        }

        if (progreso.isAprobado()) {
            throw new IllegalStateException("No se puede cancelar un curso ya aprobado.");
        }

        progreso.setEstado("CANCELADO");
        progreso.setFechaBloqueo(LocalDateTime.now().plusHours(1));
        // Resetear videoCompletado al cancelar para obligar a ver de nuevo después del bloqueo
        progreso.setVideoCompletado(false);
        progreso.setFechaInicioVideo(null);
        progreso.setFechaFinVideo(null);

        return progresoCursoRepository.save(progreso);
    }

    /**
     * Evalúa las respuestas del test y guarda el progreso resultante.
     */
    @Transactional
    public ProgresoCurso guardarResultadoTest(Long voluntarioId, Long cursoId, Map<Long, String> respuestas) {
        ProgresoCurso progreso = obtenerOCrearProgreso(voluntarioId, cursoId);

        if (estaBloqueado(progreso)) {
            throw new IllegalStateException("El curso está bloqueado por reprobación o cancelación.");
        }

        if (progreso.isAprobado()) {
            throw new IllegalStateException("Este curso ya ha sido aprobado.");
        }

        if (!progreso.isVideoCompletado()) {
            throw new IllegalStateException("Debe completar el video antes de realizar el test.");
        }

        List<Pregunta> preguntas = preguntaRepository.findByCursoId(cursoId);
        if (preguntas.isEmpty()) {
            throw new IllegalStateException("El curso no contiene preguntas para evaluar.");
        }

        int correctas = 0;
        for (Pregunta pregunta : preguntas) {
            String respuestaEnviada = respuestas.get(pregunta.getId());
            if (respuestaEnviada != null && respuestaEnviada.trim().equalsIgnoreCase(pregunta.getRespuestaCorrecta().trim())) {
                correctas++;
            }
        }

        int puntaje = correctas * 10;
        progreso.setPuntajeObtenido(puntaje);
        progreso.setFechaUltimoIntento(LocalDateTime.now());
        progreso.setIntentos(progreso.getIntentos() + 1);

        if (puntaje >= 80) { // 80% o más para aprobar (8 de 10)
            progreso.setAprobado(true);
            progreso.setEstado("APROBADO");
            progreso.setFechaBloqueo(null); // Sin bloqueo si aprueba
        } else {
            progreso.setAprobado(false);
            progreso.setEstado("REPROBADO");
            progreso.setFechaBloqueo(LocalDateTime.now().plusHours(1)); // Bloqueo de 1 hora
            // Resetear videoCompletado al reprobar para obligar a ver de nuevo después del bloqueo
            progreso.setVideoCompletado(false);
            progreso.setFechaInicioVideo(null);
            progreso.setFechaFinVideo(null);
        }

        return progresoCursoRepository.save(progreso);
    }

    /**
     * Obtiene todos los progresos de un voluntario.
     */
    public List<ProgresoCurso> obtenerProgresosDeVoluntario(Long voluntarioId) {
        return progresoCursoRepository.findByVoluntarioId(voluntarioId);
    }
}
