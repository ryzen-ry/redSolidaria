package com.redsolidaria.enjambre.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.redsolidaria.enjambre.model.*;
import com.redsolidaria.enjambre.repository.*;
import com.redsolidaria.enjambre.ws.AyudaConnectionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Servicio principal del flujo de ayuda de emergencia.
 *
 * <p>Gestiona el ciclo completo: solicitud → búsqueda de voluntario más cercano →
 * aceptación/rechazo → reenvío al siguiente candidato → finalización.
 *
 * <p>Librerías integradas:
 * <ul>
 *   <li><b>Logback (SLF4J)</b>: logging estructurado con @Slf4j en lugar de System.out/err</li>
 *   <li><b>Google Guava</b>: {@link ImmutableMap} para payloads WS, {@link Preconditions}
 *       para guardas de entrada, {@link Stopwatch} para medir latencia de búsqueda</li>
 *   <li><b>Apache Commons Lang</b>: {@link StringUtils} para comparaciones de estado,
 *       {@link Validate} para validaciones de argumentos</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
public class AyudaService {

    // Bug #2 fix: Ampliar a 10 minutos para dar margen real de uso
    private static final long MAX_UBICACION_AGE_MS = 600_000;

    private final UbicacionUsuarioRepository ubicacionUsuarioRepository;
    private final PersonaDiscapacitadaRepository personaDiscapacitadaRepository;
    private final VoluntarioRepository voluntarioRepository;
    private final SolicitudAyudaRepository solicitudAyudaRepository;
    private final SolicitudAyudaIntentoRepository solicitudAyudaIntentoRepository;
    private final UsuarioService usuarioService;
    private final AyudaConnectionRegistry connectionRegistry;
    private final HistorialAyudaRepository historialAyudaRepository;

    private final ObjectMapper objectMapper;

    public AyudaService(
            UbicacionUsuarioRepository ubicacionUsuarioRepository,
            PersonaDiscapacitadaRepository personaDiscapacitadaRepository,
            VoluntarioRepository voluntarioRepository,
            SolicitudAyudaRepository solicitudAyudaRepository,
            SolicitudAyudaIntentoRepository solicitudAyudaIntentoRepository,
            UsuarioService usuarioService,
            AyudaConnectionRegistry connectionRegistry,
            HistorialAyudaRepository historialAyudaRepository,
            ObjectMapper objectMapper
    ) {
        this.ubicacionUsuarioRepository = ubicacionUsuarioRepository;
        this.personaDiscapacitadaRepository = personaDiscapacitadaRepository;
        this.voluntarioRepository = voluntarioRepository;
        this.solicitudAyudaRepository = solicitudAyudaRepository;
        this.solicitudAyudaIntentoRepository = solicitudAyudaIntentoRepository;
        this.usuarioService = usuarioService;
        this.connectionRegistry = connectionRegistry;
        this.historialAyudaRepository = historialAyudaRepository;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UBICACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    public void actualizarUbicacion(Long usuarioId, double lat, double lng, Double precisionMetros) {
        // Commons Lang: Validate lanza IllegalArgumentException con mensaje descriptivo
        Validate.notNull(usuarioId, "usuarioId no puede ser nulo");

        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario == null) return;

        // Commons Lang: StringUtils.equalsAnyIgnoreCase evita la cadena de ||
        if (!StringUtils.equalsAnyIgnoreCase(usuario.getRol(), "DISCAPACITADO", "VOLUNTARIO")) {
            log.debug("actualizarUbicacion: rol '{}' no participa en el flujo de ayuda (usuarioId={})",
                    usuario.getRol(), usuarioId);
            return;
        }

        UbicacionUsuario ubicacion = ubicacionUsuarioRepository.findByUsuario_Id(usuarioId).orElse(null);
        if (ubicacion == null) {
            ubicacion = new UbicacionUsuario();
            ubicacion.setUsuario(usuario);
        }

        ubicacion.setLatitud(lat);
        ubicacion.setLongitud(lng);
        ubicacion.setPrecisionMetros(precisionMetros);
        ubicacion.setActualizadoEn(LocalDateTime.now());
        ubicacionUsuarioRepository.save(ubicacion);

        log.debug("Ubicación actualizada — usuario={} rol={} lat={} lng={}", usuarioId, usuario.getRol(), lat, lng);

        // Bug #6 fix: Propagar ubicación en tiempo real si hay una sesión ACEPTADA activa
        propagarUbicacionEnSesionActiva(usuarioId, usuario.getRol(), lat, lng, precisionMetros);
    }

    /**
     * Bug #6 fix: Si hay una solicitud ACEPTADA en curso, reenvía la nueva
     * ubicación a la otra parte en tiempo real.
     */
    private void propagarUbicacionEnSesionActiva(Long usuarioId, String rol,
                                                  double lat, double lng, Double precisionMetros) {
        // Guava: ImmutableMap.builder() construye el payload de forma inmutable y legible
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("type", "UBICACION_ACTUALIZADA");
        builder.put("lat", lat);
        builder.put("lng", lng);
        if (precisionMetros != null) builder.put("precisionMetros", precisionMetros);
        Map<String, Object> payloadUbicacion = builder.build();

        if (StringUtils.equalsIgnoreCase(rol, "DISCAPACITADO")) {
            solicitudAyudaRepository
                    .findTopByDiscapacitado_IdAndEstadoOrderByAceptadaEnDesc(usuarioId, "ACEPTADA")
                    .ifPresent(solicitud -> {
                        if (solicitud.getVoluntarioAceptado() != null) {
                            connectionRegistry.sendToUser(solicitud.getVoluntarioAceptado().getId(), payloadUbicacion);
                            log.debug("Ubicación del discapacitado {} propagada al voluntario {}",
                                    usuarioId, solicitud.getVoluntarioAceptado().getId());
                        }
                    });
        } else if (StringUtils.equalsIgnoreCase(rol, "VOLUNTARIO")) {
            solicitudAyudaRepository
                    .findTopByVoluntarioAceptado_IdAndEstadoOrderByAceptadaEnDesc(usuarioId, "ACEPTADA")
                    .ifPresent(solicitud -> {
                        connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payloadUbicacion);
                        log.debug("Ubicación del voluntario {} propagada al discapacitado {}",
                                usuarioId, solicitud.getDiscapacitado().getId());
                    });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SOLICITAR AYUDA
    // ─────────────────────────────────────────────────────────────────────────

    public SolicitudAyuda solicitarAyuda(Long discapacitadoId) {
        // Guava: Preconditions.checkArgument lanza IllegalArgumentException con mensaje
        Preconditions.checkArgument(discapacitadoId != null, "discapacitadoId no puede ser nulo");

        PersonaDiscapacitada discapacitado = personaDiscapacitadaRepository.findById(discapacitadoId)
                .orElseThrow(() -> new IllegalArgumentException("Discapacitado no encontrado"));

        // Bug #5 fix: Evitar solicitudes duplicadas si ya hay una PENDIENTE activa
        Optional<SolicitudAyuda> pendienteExistente = solicitudAyudaRepository
                .findTopByDiscapacitado_IdAndEstadoOrderByCreadaEnDesc(discapacitadoId, "PENDIENTE");
        if (pendienteExistente.isPresent()) {
            log.info("[solicitarAyuda] Reutilizando solicitud PENDIENTE existente id={} para discapacitado={}",
                    pendienteExistente.get().getId(), discapacitadoId);
            return pendienteExistente.get();
        }

        UbicacionUsuario ubicacionDis = ubicacionUsuarioRepository.findByUsuario_Id(discapacitadoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ubicación no disponible. Activa tu GPS e inténtalo nuevamente."));

        // Bug #2 fix: ahora MAX_UBICACION_AGE_MS = 600_000 (10 minutos)
        if (ubicacionDis.getActualizadoEn() == null ||
                ubicacionDis.getActualizadoEn().isBefore(LocalDateTime.now().minusNanos(MAX_UBICACION_AGE_MS * 1_000_000))) {
            throw new IllegalArgumentException("Tu ubicación no está actualizada. Activa tu GPS e inténtalo nuevamente.");
        }

        SolicitudAyuda solicitud = new SolicitudAyuda();
        solicitud.setDiscapacitado(discapacitado);
        solicitud.setVoluntarioAceptado(null);
        solicitud.setEstado("PENDIENTE");
        solicitud.setCreadaEn(LocalDateTime.now());

        SolicitudAyuda guardada = solicitudAyudaRepository.save(solicitud);
        log.info("[solicitarAyuda] Nueva solicitud id={} creada para discapacitado={}", guardada.getId(), discapacitadoId);

        // Enviar la primera alerta al voluntario más cercano
        enviarSiguienteVoluntario(guardada.getId(), ubicacionDis);

        return guardada;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESPONDER (ACEPTAR / RECHAZAR)
    // ─────────────────────────────────────────────────────────────────────────

    public void responderAyuda(Long solicitudId, Long voluntarioId, String decision) {
        Preconditions.checkArgument(solicitudId != null,  "solicitudId no puede ser nulo");
        Preconditions.checkArgument(voluntarioId != null, "voluntarioId no puede ser nulo");
        // Commons Lang: StringUtils.isNotBlank valida cadena no nula y no vacía
        Preconditions.checkArgument(StringUtils.isNotBlank(decision), "decision no puede estar vacía");

        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!StringUtils.equals(solicitud.getEstado(), "PENDIENTE")) {
            log.warn("[responderAyuda] Solicitud {} ya no está PENDIENTE (estado={}). Ignorando.",
                    solicitudId, solicitud.getEstado());
            return;
        }

        SolicitudAyudaIntento intento = solicitudAyudaIntentoRepository
                .findBySolicitud_IdAndVoluntario_Id(solicitudId, voluntarioId)
                .orElse(null);

        if (intento == null) {
            log.warn("[responderAyuda] No existe intento para solicitud={} voluntario={}. Ignorando.",
                    solicitudId, voluntarioId);
            return;
        }
        if (!StringUtils.equals(intento.getEstado(), "PENDIENTE")) {
            log.warn("[responderAyuda] Intento ya respondido (estado={}). Ignorando.", intento.getEstado());
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (StringUtils.equalsIgnoreCase(decision, "ACEPTAR")) {
            handleAceptar(solicitud, intento, voluntarioId, ahora);
            return;
        }

        if (StringUtils.equalsIgnoreCase(decision, "RECHAZAR")) {
            handleRechazar(solicitud, intento, voluntarioId, solicitudId, ahora);
        }
    }

    /** Lógica de aceptación extraída para mayor legibilidad. */
    private void handleAceptar(SolicitudAyuda solicitud, SolicitudAyudaIntento intento,
                                Long voluntarioId, LocalDateTime ahora) {
        // Bug #3 fix: verificar que el voluntario tiene ubicación activa ANTES de aceptar
        UbicacionUsuario ubicacionVol = ubicacionUsuarioRepository.findByUsuario_Id(voluntarioId).orElse(null);
        if (ubicacionVol == null) {
            Map<String, Object> errVol = ImmutableMap.of(
                    "type", "ERROR",
                    "mensaje", "Debes tener el GPS activo para aceptar solicitudes. Activa tu ubicación y vuelve a intentarlo."
            );
            connectionRegistry.sendToUser(voluntarioId, errVol);
            log.warn("[handleAceptar] Voluntario {} intentó aceptar sin GPS activo.", voluntarioId);
            return;
        }

        UbicacionUsuario ubicacionDis = ubicacionUsuarioRepository
                .findByUsuario_Id(solicitud.getDiscapacitado().getId()).orElse(null);
        if (ubicacionDis == null) {
            Map<String, Object> errVol = ImmutableMap.of(
                    "type", "ERROR",
                    "mensaje", "El beneficiario aún no tiene ubicación GPS activa. Espera unos segundos e intenta de nuevo."
            );
            connectionRegistry.sendToUser(voluntarioId, errVol);
            log.warn("[handleAceptar] Discapacitado {} no tiene ubicación activa.", solicitud.getDiscapacitado().getId());
            return;
        }

        intento.setEstado("ACEPTADA");
        intento.setRespondidaEn(ahora);
        solicitudAyudaIntentoRepository.save(intento);

        // Bug #1 fix: cargar Voluntario desde repositorio en lugar de hacer cast directo
        Voluntario voluntario = voluntarioRepository.findById(voluntarioId)
                .orElseThrow(() -> new IllegalArgumentException("Voluntario no encontrado"));

        solicitud.setVoluntarioAceptado(voluntario);
        solicitud.setEstado("ACEPTADA");
        solicitud.setAceptadaEn(ahora);
        solicitudAyudaRepository.save(solicitud);

        log.info("[handleAceptar] Solicitud {} ACEPTADA por voluntario {}", solicitud.getId(), voluntarioId);

        // Payload para el discapacitado: datos del voluntario + su ubicación
        Map<String, Object> payloadDis = new HashMap<>();
        payloadDis.put("type", "SOLICITUD_ACEPTADA");
        payloadDis.put("solicitudId", solicitud.getId());
        payloadDis.put("voluntario", mapVoluntario(voluntario));
        payloadDis.put("ubicacionVoluntario", mapUbicacion(ubicacionVol));
        connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payloadDis);

        // Payload para el voluntario: datos del discapacitado + su ubicación
        Map<String, Object> payloadVol = new HashMap<>();
        payloadVol.put("type", "CONFIRMACION_ACEPTACION");
        payloadVol.put("solicitudId", solicitud.getId());
        payloadVol.put("discapacitado", mapDiscapacitado(solicitud.getDiscapacitado()));
        payloadVol.put("ubicacionDiscapacitado", mapUbicacion(ubicacionDis));
        connectionRegistry.sendToUser(voluntario.getId(), payloadVol);
    }

    /** Lógica de rechazo y reenvío al siguiente voluntario. */
    private void handleRechazar(SolicitudAyuda solicitud, SolicitudAyudaIntento intento,
                                 Long voluntarioId, Long solicitudId, LocalDateTime ahora) {
        intento.setEstado("RECHAZADA");
        intento.setRespondidaEn(ahora);
        solicitudAyudaIntentoRepository.save(intento);

        log.info("[handleRechazar] Voluntario {} rechazó solicitud {}. Buscando siguiente candidato.",
                voluntarioId, solicitudId);

        // Notificar al voluntario que rechazó
        Map<String, Object> payloadRechazoVol = ImmutableMap.of(
                "type", "SOLICITUD_RECHAZADA",
                "solicitudId", solicitudId
        );
        connectionRegistry.sendToUser(voluntarioId, payloadRechazoVol);

        // Notificar al discapacitado que se está buscando otro voluntario
        Map<String, Object> payloadRechazoDis = ImmutableMap.of(
                "type", "SOLICITUD_RECHAZADA",
                "solicitudId", solicitudId,
                "mensaje", "El voluntario rechazó la solicitud. Buscando otro cercano..."
        );
        connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payloadRechazoDis);

        // ✅ NÚCLEO: enviar al siguiente voluntario más cercano no intentado
        enviarSiguienteVoluntario(solicitudId, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CANCELAR SOLICITUD
    // ─────────────────────────────────────────────────────────────────────────

    public void cancelarSolicitud(Long solicitudId, Long discapacitadoId) {
        if (solicitudId == null || discapacitadoId == null) return;

        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId).orElse(null);
        if (solicitud == null) return;
        if (solicitud.getDiscapacitado() == null || !discapacitadoId.equals(solicitud.getDiscapacitado().getId())) {
            return;
        }
        if (!StringUtils.equals(solicitud.getEstado(), "PENDIENTE")) {
            log.debug("[cancelarSolicitud] Solicitud {} no está PENDIENTE (estado={}). Ignorando.",
                    solicitudId, solicitud.getEstado());
            return;
        }

        solicitud.setEstado("CANCELADA");
        solicitudAyudaRepository.save(solicitud);
        log.info("[cancelarSolicitud] Solicitud {} cancelada por discapacitado {}", solicitudId, discapacitadoId);

        // Cancelar intentos pendientes y notificar a los voluntarios para que retiren la tarjeta
        List<SolicitudAyudaIntento> intentos = solicitudAyudaIntentoRepository.findBySolicitud_Id(solicitudId);
        for (SolicitudAyudaIntento i : intentos) {
            if (i == null) continue;
            if (StringUtils.equals(i.getEstado(), "PENDIENTE")) {
                i.setEstado("CANCELADA");
                i.setRespondidaEn(LocalDateTime.now());
                solicitudAyudaIntentoRepository.save(i);
            }

            if (i.getVoluntario() != null) {
                // Bug #7 fix: usar tipo específico para cancelación por el usuario
                Map<String, Object> payloadVol = ImmutableMap.of(
                        "type", "SOLICITUD_CANCELADA_POR_USUARIO",
                        "solicitudId", solicitudId,
                        "mensaje", "La solicitud fue cancelada por el usuario."
                );
                connectionRegistry.sendToUser(i.getVoluntario().getId(), payloadVol);
            }
        }

        Map<String, Object> payloadDis = ImmutableMap.of(
                "type", "SOLICITUD_CANCELADA",
                "solicitudId", solicitudId,
                "mensaje", "Solicitud cancelada."
        );
        connectionRegistry.sendToUser(discapacitadoId, payloadDis);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BÚSQUEDA DEL SIGUIENTE VOLUNTARIO MÁS CERCANO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca el voluntario conectado más cercano que aún no haya sido contactado
     * para esta solicitud, y le envía la alerta.
     *
     * <p>Si no hay candidatos disponibles, cancela la solicitud y notifica al discapacitado.
     * Guava {@link Stopwatch} registra la latencia de la búsqueda.
     */
    private void enviarSiguienteVoluntario(Long solicitudId, UbicacionUsuario ubicacionDisCache) {
        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId).orElse(null);
        if (solicitud == null) return;
        if (!StringUtils.equals(solicitud.getEstado(), "PENDIENTE")) return;

        // Guava: Stopwatch para medir latencia de la búsqueda del candidato
        Stopwatch stopwatch = Stopwatch.createStarted();

        LocalDateTime ahora  = LocalDateTime.now();
        LocalDateTime cutoff = ahora.minusNanos(MAX_UBICACION_AGE_MS * 1_000_000);

        UbicacionUsuario ubicacionDis = ubicacionDisCache != null
                ? ubicacionDisCache
                : ubicacionUsuarioRepository.findByUsuario_Id(solicitud.getDiscapacitado().getId()).orElse(null);

        if (ubicacionDis == null) {
            solicitud.setEstado("CANCELADA");
            solicitudAyudaRepository.save(solicitud);
            Map<String, Object> payload = ImmutableMap.of(
                    "type", "SOLICITUD_CANCELADA",
                    "solicitudId", solicitudId,
                    "mensaje", "No se encontró tu ubicación. Intenta nuevamente."
            );
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
            log.warn("[enviarSiguienteVoluntario] Solicitud {} cancelada: discapacitado sin ubicación.", solicitudId);
            return;
        }

        List<UbicacionUsuario> voluntariosActivos = ubicacionUsuarioRepository
                .findByUsuario_RolAndActualizadoEnAfter("VOLUNTARIO", cutoff);

        // Evitar enviarle la misma solicitud a voluntarios que ya fueron intentados
        List<SolicitudAyudaIntento> intentos = solicitudAyudaIntentoRepository.findBySolicitud_Id(solicitudId);
        Set<Long> voluntariosIntentados = new HashSet<>();
        for (SolicitudAyudaIntento i : intentos) {
            if (i.getVoluntario() != null) voluntariosIntentados.add(i.getVoluntario().getId());
        }

        log.debug("[enviarSiguienteVoluntario] Solicitud={} | voluntariosActivos={} | yaIntentados={}",
                solicitudId, voluntariosActivos.size(), voluntariosIntentados.size());

        UbicacionUsuario mejor = null;
        double mejorDistanciaKm = Double.MAX_VALUE;

        for (UbicacionUsuario ubicVol : voluntariosActivos) {
            if (ubicVol.getUsuario() == null) continue;
            if (voluntariosIntentados.contains(ubicVol.getUsuario().getId())) continue;
            if (!connectionRegistry.isUserConnected(ubicVol.getUsuario().getId())) continue;

            double distKm = calcularDistanciaKm(
                    ubicacionDis.getLatitud(), ubicacionDis.getLongitud(),
                    ubicVol.getLatitud(), ubicVol.getLongitud()
            );

            if (distKm < mejorDistanciaKm) {
                mejorDistanciaKm = distKm;
                mejor = ubicVol;
            }
        }

        stopwatch.stop();
        log.debug("[enviarSiguienteVoluntario] Búsqueda completada en {} ms",
                stopwatch.elapsed(TimeUnit.MILLISECONDS));

        if (mejor == null) {
            solicitud.setEstado("CANCELADA");
            solicitudAyudaRepository.save(solicitud);
            Map<String, Object> payload = ImmutableMap.of(
                    "type", "SOLICITUD_CANCELADA",
                    "solicitudId", solicitudId,
                    "mensaje", "No hay voluntarios conectados y disponibles cerca en este momento."
            );
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
            log.warn("[enviarSiguienteVoluntario] Solicitud {} sin candidatos disponibles. Cancelada.", solicitudId);
            return;
        }

        // Bug #1 fix: cargar el Voluntario correctamente desde su repositorio
        Long candidatoId = mejor.getUsuario().getId();
        Voluntario voluntarioCandidato = voluntarioRepository.findById(candidatoId).orElse(null);
        if (voluntarioCandidato == null) {
            log.error("[enviarSiguienteVoluntario] Voluntario id={} no encontrado en tabla voluntarios. Cancelando solicitud {}.",
                    candidatoId, solicitudId);
            solicitud.setEstado("CANCELADA");
            solicitudAyudaRepository.save(solicitud);
            Map<String, Object> payload = ImmutableMap.of(
                    "type", "SOLICITUD_CANCELADA",
                    "solicitudId", solicitudId,
                    "mensaje", "Error interno buscando voluntario. Intenta nuevamente."
            );
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
            return;
        }

        SolicitudAyudaIntento intento = new SolicitudAyudaIntento();
        intento.setSolicitud(solicitud);
        intento.setVoluntario(voluntarioCandidato);
        intento.setEstado("PENDIENTE");
        intento.setCreadaEn(LocalDateTime.now());
        intento.setRespondidaEn(null);
        solicitudAyudaIntentoRepository.save(intento);

        // Guava: ImmutableMap para el payload al voluntario candidato
        double distRedondeada = Math.round(mejorDistanciaKm * 100.0) / 100.0;
        Map<String, Object> payloadVol = new HashMap<>();
        payloadVol.put("type", "NUEVA_SOLICITUD");
        payloadVol.put("solicitudId", solicitudId);
        payloadVol.put("discapacitado", mapDiscapacitado(solicitud.getDiscapacitado()));
        payloadVol.put("ubicacionDiscapacitado", mapUbicacion(ubicacionDis));
        payloadVol.put("distanciaKm", distRedondeada);
        connectionRegistry.sendToUser(voluntarioCandidato.getId(), payloadVol);

        log.info("[enviarSiguienteVoluntario] Solicitud {} → voluntario {} (dist={} km)",
                solicitudId, voluntarioCandidato.getId(), distRedondeada);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TERMINAR AYUDA
    // ─────────────────────────────────────────────────────────────────────────

    public void terminarAyuda(Long solicitudId, Long usuarioId) {
        Preconditions.checkArgument(solicitudId != null,  "solicitudId no puede ser nulo");
        Preconditions.checkArgument(usuarioId != null,    "usuarioId no puede ser nulo");

        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!StringUtils.equals(solicitud.getEstado(), "ACEPTADA")) {
            throw new IllegalStateException("La solicitud no se encuentra en curso (estado ACEPTADA)");
        }

        boolean isDiscapacitado = solicitud.getDiscapacitado() != null
                && usuarioId.equals(solicitud.getDiscapacitado().getId());
        boolean isVoluntario = solicitud.getVoluntarioAceptado() != null
                && usuarioId.equals(solicitud.getVoluntarioAceptado().getId());

        if (!isDiscapacitado && !isVoluntario) {
            throw new IllegalArgumentException("No estás autorizado para terminar esta ayuda");
        }

        solicitud.setEstado("FINALIZADA");
        solicitudAyudaRepository.save(solicitud);

        HistorialAyuda historial = new HistorialAyuda(solicitud);
        historialAyudaRepository.save(historial);

        log.info("[terminarAyuda] Solicitud {} finalizada por usuario {}", solicitudId, usuarioId);

        Map<String, Object> payload = ImmutableMap.of(
                "type", "AYUDA_FINALIZADA",
                "solicitudId", solicitudId,
                "mensaje", "La ayuda ha sido completada correctamente."
        );

        if (solicitud.getDiscapacitado() != null) {
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
        }
        if (solicitud.getVoluntarioAceptado() != null) {
            connectionRegistry.sendToUser(solicitud.getVoluntarioAceptado().getId(), payload);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS DE MAPEO
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> mapUbicacion(UbicacionUsuario u) {
        Map<String, Object> m = new HashMap<>();
        m.put("lat", u.getLatitud());
        m.put("lng", u.getLongitud());
        if (u.getPrecisionMetros() != null) m.put("precisionMetros", u.getPrecisionMetros());
        return m;
    }

    private Map<String, Object> mapVoluntario(Voluntario v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("nombres", v.getNombres());
        m.put("apellidos", v.getApellidos());
        m.put("email", v.getEmail());
        if (v.getFotoPerfil() != null) m.put("fotoPerfil", v.getFotoPerfil());
        return m;
    }

    private Map<String, Object> mapDiscapacitado(PersonaDiscapacitada d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("nombres", d.getNombres());
        m.put("apellidos", d.getApellidos());
        m.put("telefono", d.getTelefono());
        m.put("direccion", d.getDireccion());
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CÁLCULO DE DISTANCIA (Haversine)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcula la distancia en kilómetros entre dos coordenadas geográficas
     * usando la fórmula de Haversine.
     */
    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
