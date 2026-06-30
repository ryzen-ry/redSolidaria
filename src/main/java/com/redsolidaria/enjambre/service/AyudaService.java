package com.redsolidaria.enjambre.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redsolidaria.enjambre.model.*;
import com.redsolidaria.enjambre.repository.*;
import com.redsolidaria.enjambre.ws.AyudaConnectionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

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
    private final EmailService emailService;
    private final IncidenciaRepository incidenciaRepository;

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
            EmailService emailService,
            IncidenciaRepository incidenciaRepository,
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
        this.emailService = emailService;
        this.incidenciaRepository = incidenciaRepository;
        this.objectMapper = objectMapper;
    }

    public void actualizarUbicacion(Long usuarioId, double lat, double lng, Double precisionMetros) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario == null) return;

        // Solo guardamos ubicaciones de los roles que participan.
        if (!"DISCAPACITADO".equals(usuario.getRol()) && !"VOLUNTARIO".equals(usuario.getRol())) {
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

        // Bug #6 fix: Propagar ubicación en tiempo real si hay una sesión ACEPTADA activa
        propagarUbicacionEnSesionActiva(usuarioId, usuario.getRol(), lat, lng, precisionMetros);
    }

    /**
     * Bug #6 fix: Si hay una solicitud ACEPTADA en curso, reenvía la nueva
     * ubicación a la otra parte en tiempo real.
     */
    private void propagarUbicacionEnSesionActiva(Long usuarioId, String rol,
                                                  double lat, double lng, Double precisionMetros) {
        Map<String, Object> payloadUbicacion = new HashMap<>();
        payloadUbicacion.put("type", "UBICACION_ACTUALIZADA");
        payloadUbicacion.put("lat", lat);
        payloadUbicacion.put("lng", lng);
        if (precisionMetros != null) payloadUbicacion.put("precisionMetros", precisionMetros);

        if ("DISCAPACITADO".equals(rol)) {
            solicitudAyudaRepository
                    .findTopByDiscapacitado_IdAndEstadoOrderByAceptadaEnDesc(usuarioId, "ACEPTADA")
                    .ifPresent(solicitud -> {
                        if (solicitud.getVoluntarioAceptado() != null) {
                            connectionRegistry.sendToUser(solicitud.getVoluntarioAceptado().getId(), payloadUbicacion);
                        }
                    });
        } else if ("VOLUNTARIO".equals(rol)) {
            solicitudAyudaRepository
                    .findTopByVoluntarioAceptado_IdAndEstadoOrderByAceptadaEnDesc(usuarioId, "ACEPTADA")
                    .ifPresent(solicitud ->
                            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payloadUbicacion));
        }
    }

    public SolicitudAyuda solicitarAyuda(Long discapacitadoId) {
        PersonaDiscapacitada discapacitado = personaDiscapacitadaRepository.findById(discapacitadoId)
                .orElseThrow(() -> new IllegalArgumentException("Discapacitado no encontrado"));

        // Bug #5 fix: Evitar solicitudes duplicadas si ya hay una PENDIENTE activa
        Optional<SolicitudAyuda> pendienteExistente = solicitudAyudaRepository
                .findTopByDiscapacitado_IdAndEstadoOrderByCreadaEnDesc(discapacitadoId, "PENDIENTE");
        if (pendienteExistente.isPresent()) {
            // Devolver la solicitud existente en lugar de crear una nueva
            System.out.println("[AyudaService] Ya existe solicitud PENDIENTE " + pendienteExistente.get().getId()
                    + " para discapacitado " + discapacitadoId + ". Reutilizando.");
            return pendienteExistente.get();
        }

        UbicacionUsuario ubicacionDis = ubicacionUsuarioRepository.findByUsuario_Id(discapacitadoId)
                .orElseThrow(() -> new IllegalArgumentException("Ubicación no disponible. Activa tu GPS e inténtalo nuevamente."));

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

        // Enviar la primera alerta al voluntario más cercano.
        enviarSiguienteVoluntario(guardada.getId(), ubicacionDis);

        return guardada;
    }

    public void responderAyuda(Long solicitudId, Long voluntarioId, String decision) {
        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            return;
        }

        SolicitudAyudaIntento intento = solicitudAyudaIntentoRepository
                .findBySolicitud_IdAndVoluntario_Id(solicitudId, voluntarioId)
                .orElse(null);

        if (intento == null) return;
        if (!"PENDIENTE".equals(intento.getEstado())) return;

        LocalDateTime ahora = LocalDateTime.now();

        if ("ACEPTAR".equalsIgnoreCase(decision)) {
            // Bug #3 fix: verificar que el voluntario tiene ubicación activa ANTES de aceptar
            UbicacionUsuario ubicacionVol = ubicacionUsuarioRepository.findByUsuario_Id(voluntarioId).orElse(null);
            if (ubicacionVol == null) {
                Map<String, Object> errVol = new HashMap<>();
                errVol.put("type", "ERROR");
                errVol.put("mensaje", "Debes tener el GPS activo para aceptar solicitudes. Activa tu ubicación y vuelve a intentarlo.");
                connectionRegistry.sendToUser(voluntarioId, errVol);
                return;
            }

            UbicacionUsuario ubicacionDis = ubicacionUsuarioRepository
                    .findByUsuario_Id(solicitud.getDiscapacitado().getId()).orElse(null);
            if (ubicacionDis == null) {
                Map<String, Object> errVol = new HashMap<>();
                errVol.put("type", "ERROR");
                errVol.put("mensaje", "El beneficiario aún no tiene ubicación GPS activa. Espera unos segundos e intenta de nuevo.");
                connectionRegistry.sendToUser(voluntarioId, errVol);
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

            // Payload para el discapacitado: datos del voluntario + su ubicación
            Map<String, Object> payloadDis = new HashMap<>();
            payloadDis.put("type", "SOLICITUD_ACEPTADA");
            payloadDis.put("solicitudId", solicitud.getId());
            payloadDis.put("voluntario", mapVoluntario(voluntario));
            payloadDis.put("ubicacionVoluntario", mapUbicacion(ubicacionVol)); // Siempre presente (validado arriba)

            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payloadDis);

            // Payload para el voluntario: datos del discapacitado + su ubicación
            Map<String, Object> payloadVol = new HashMap<>();
            payloadVol.put("type", "CONFIRMACION_ACEPTACION");
            payloadVol.put("solicitudId", solicitud.getId());
            payloadVol.put("discapacitado", mapDiscapacitado(solicitud.getDiscapacitado()));
            payloadVol.put("ubicacionDiscapacitado", mapUbicacion(ubicacionDis));

            connectionRegistry.sendToUser(voluntario.getId(), payloadVol);

            return;
        }

        if ("RECHAZAR".equalsIgnoreCase(decision)) {
            intento.setEstado("RECHAZADA");
            intento.setRespondidaEn(ahora);
            solicitudAyudaIntentoRepository.save(intento);

            // Avisar al voluntario que rechazó.
            Map<String, Object> payloadRechazoVol = new HashMap<>();
            payloadRechazoVol.put("type", "SOLICITUD_RECHAZADA");
            payloadRechazoVol.put("solicitudId", solicitudId);
            connectionRegistry.sendToUser(voluntarioId, payloadRechazoVol);

            // También avisar al discapacitado para que vea que se está buscando a otro.
            Map<String, Object> payloadRechazoDis = new HashMap<>();
            payloadRechazoDis.put("type", "SOLICITUD_RECHAZADA");
            payloadRechazoDis.put("solicitudId", solicitudId);
            payloadRechazoDis.put("mensaje", "El voluntario rechazó la solicitud. Buscando otro cercano...");
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payloadRechazoDis);

            // Enviar al siguiente más cercano.
            enviarSiguienteVoluntario(solicitudId, null);
        }
    }

    public void cancelarSolicitud(Long solicitudId, Long discapacitadoId) {
        if (solicitudId == null || discapacitadoId == null) return;

        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId).orElse(null);
        if (solicitud == null) return;
        if (solicitud.getDiscapacitado() == null || !discapacitadoId.equals(solicitud.getDiscapacitado().getId())) {
            return;
        }
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            return;
        }

        solicitud.setEstado("CANCELADA");
        solicitudAyudaRepository.save(solicitud);

        // Cancelar intentos pendientes y notificar a los voluntarios para que retiren la tarjeta.
        List<SolicitudAyudaIntento> intentos = solicitudAyudaIntentoRepository.findBySolicitud_Id(solicitudId);
        for (SolicitudAyudaIntento i : intentos) {
            if (i == null) continue;
            if ("PENDIENTE".equals(i.getEstado())) {
                i.setEstado("CANCELADA");
                i.setRespondidaEn(LocalDateTime.now());
                solicitudAyudaIntentoRepository.save(i);
            }

            if (i.getVoluntario() != null) {
                Map<String, Object> payloadVol = new HashMap<>();
                // Bug #7 fix: usar tipo específico para cancelación por el usuario
                payloadVol.put("type", "SOLICITUD_CANCELADA_POR_USUARIO");
                payloadVol.put("solicitudId", solicitudId);
                payloadVol.put("mensaje", "La solicitud fue cancelada por el usuario.");
                connectionRegistry.sendToUser(i.getVoluntario().getId(), payloadVol);
            }
        }

        Map<String, Object> payloadDis = new HashMap<>();
        payloadDis.put("type", "SOLICITUD_CANCELADA");
        payloadDis.put("solicitudId", solicitudId);
        payloadDis.put("mensaje", "Solicitud cancelada.");
        connectionRegistry.sendToUser(discapacitadoId, payloadDis);
    }

    private void enviarSiguienteVoluntario(Long solicitudId, UbicacionUsuario ubicacionDisCache) {
        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId).orElse(null);
        if (solicitud == null) return;
        if (!"PENDIENTE".equals(solicitud.getEstado())) return;

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime cutoff = ahora.minusNanos(MAX_UBICACION_AGE_MS * 1_000_000);

        UbicacionUsuario ubicacionDis = ubicacionDisCache != null
                ? ubicacionDisCache
                : ubicacionUsuarioRepository.findByUsuario_Id(solicitud.getDiscapacitado().getId()).orElse(null);

        if (ubicacionDis == null) {
            solicitud.setEstado("CANCELADA");
            solicitudAyudaRepository.save(solicitud);
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SOLICITUD_CANCELADA");
            payload.put("solicitudId", solicitudId);
            payload.put("mensaje", "No se encontró tu ubicación. Intenta nuevamente.");
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
            return;
        }

        List<UbicacionUsuario> voluntariosActivos = ubicacionUsuarioRepository
                .findByUsuario_RolAndActualizadoEnAfter("VOLUNTARIO", cutoff);

        // Evitar enviarle la misma solicitud a voluntarios que ya fueron intentados.
        List<SolicitudAyudaIntento> intentos = solicitudAyudaIntentoRepository.findBySolicitud_Id(solicitudId);
        Set<Long> voluntariosIntentados = new HashSet<>();
        for (SolicitudAyudaIntento i : intentos) {
            if (i.getVoluntario() != null) voluntariosIntentados.add(i.getVoluntario().getId());
        }

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

        if (mejor == null) {
            solicitud.setEstado("CANCELADA");
            solicitudAyudaRepository.save(solicitud);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SOLICITUD_CANCELADA");
            payload.put("solicitudId", solicitudId);
            payload.put("mensaje", "No hay voluntarios conectados y disponibles cerca en este momento.");
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
            return;
        }

        // Bug #1 fix: cargar el Voluntario correctamente desde su repositorio
        // en lugar de hacer cast directo desde el proxy de UbicacionUsuario.usuario
        Long candidatoId = mejor.getUsuario().getId();
        Voluntario voluntarioCandidato = voluntarioRepository.findById(candidatoId).orElse(null);
        if (voluntarioCandidato == null) {
            System.err.println("[AyudaService] Voluntario con id=" + candidatoId
                    + " no encontrado en tabla voluntarios. Saltando.");
            // Marcar como intentado para no volver a seleccionarlo y buscar el siguiente
            SolicitudAyudaIntento intentoFallido = new SolicitudAyudaIntento();
            intentoFallido.setSolicitud(solicitud);
            // No podemos continuar sin el voluntario, cancelar la solicitud.
            solicitud.setEstado("CANCELADA");
            solicitudAyudaRepository.save(solicitud);
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SOLICITUD_CANCELADA");
            payload.put("solicitudId", solicitudId);
            payload.put("mensaje", "Error interno buscando voluntario. Intenta nuevamente.");
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

        Map<String, Object> payloadVol = new HashMap<>();
        payloadVol.put("type", "NUEVA_SOLICITUD");
        payloadVol.put("solicitudId", solicitudId);
        payloadVol.put("discapacitado", mapDiscapacitado(solicitud.getDiscapacitado()));
        payloadVol.put("ubicacionDiscapacitado", mapUbicacion(ubicacionDis));
        payloadVol.put("distanciaKm", Math.round(mejorDistanciaKm * 100.0) / 100.0);

        connectionRegistry.sendToUser(voluntarioCandidato.getId(), payloadVol);

        System.out.println("[AyudaService] Solicitud " + solicitudId
                + " enviada a voluntario " + voluntarioCandidato.getId()
                + " (dist=" + mejorDistanciaKm + " km)");
    }

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
        m.put("carrera", v.getCarrera());
        m.put("codigo", v.getCodigo());
        if (v.getFotoPerfil() != null) m.put("fotoPerfil", v.getFotoPerfil());
        return m;
    }

    private Map<String, Object> mapDiscapacitado(PersonaDiscapacitada d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("nombres", d.getNombres());
        m.put("apellidos", d.getApellidos());
        m.put("telefono", d.getTelefono());
        m.put("tipoDiscapacidad", d.getTipoDiscapacidad());
        m.put("conadis", d.getConadis());
        return m;
    }

    /**
     * Registra la calificacion y comentario del discapacitado hacia el voluntario.
     * Solo se puede comentar una vez por historial.
     */
    public void registrarComentarioDiscapacitado(Long historialId, String calificacion,
                                                  String comentario, Long discapacitadoId) {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));

        // Verificar que el discapacitado es el dueño del historial
        if (historial.getSolicitud().getDiscapacitado() == null ||
                !discapacitadoId.equals(historial.getSolicitud().getDiscapacitado().getId())) {
            throw new IllegalArgumentException("No autorizado para comentar este historial");
        }

        if (historial.isComentadoDiscapacitado()) {
            throw new IllegalStateException("Ya has comentado esta asistencia");
        }

        // Validar calificacion
        if (calificacion != null && !calificacion.isBlank()) {
            if (!"Oro".equals(calificacion) && !"Plata".equals(calificacion) && !"Cobre".equals(calificacion)) {
                throw new IllegalArgumentException("Calificacion invalida. Use: Oro, Plata o Cobre");
            }
            historial.setCalificacion(calificacion);
        }

        historial.setComentarioDiscapacitado(comentario);
        historial.setComentadoDiscapacitado(true);
        historialAyudaRepository.save(historial);

        // Notificar por correo al voluntario
        Voluntario voluntario = historial.getSolicitud().getVoluntarioAceptado();
        if (voluntario != null && voluntario.getEmail() != null) {
            String nombreDis = historial.getSolicitud().getDiscapacitado().getNombres()
                    + " " + historial.getSolicitud().getDiscapacitado().getApellidos();
            emailService.enviarComentarioAVoluntario(voluntario.getEmail(), nombreDis, calificacion, comentario);
        }
    }

    /**
     * Registra el comentario del voluntario hacia el discapacitado.
     * Solo se puede comentar una vez por historial.
     */
    public void registrarComentarioVoluntario(Long historialId, String comentario, Long voluntarioId) {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));

        // Verificar que el voluntario es el asignado del historial
        if (historial.getSolicitud().getVoluntarioAceptado() == null ||
                !voluntarioId.equals(historial.getSolicitud().getVoluntarioAceptado().getId())) {
            throw new IllegalArgumentException("No autorizado para comentar este historial");
        }

        if (historial.isComentadoVoluntario()) {
            throw new IllegalStateException("Ya has comentado esta asistencia");
        }

        historial.setComentarioVoluntario(comentario);
        historial.setComentadoVoluntario(true);
        historialAyudaRepository.save(historial);

        // Notificar por correo al discapacitado
        PersonaDiscapacitada discapacitado = historial.getSolicitud().getDiscapacitado();
        if (discapacitado != null && discapacitado.getEmail() != null) {
            String nombreVol = historial.getSolicitud().getVoluntarioAceptado().getNombres()
                    + " " + historial.getSolicitud().getVoluntarioAceptado().getApellidos();
            emailService.enviarComentarioADiscapacitado(discapacitado.getEmail(), nombreVol, comentario);
        }
    }

    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        // Fórmula Haversine para distancias en la Tierra.
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void terminarAyuda(Long solicitudId, Long usuarioId) {
        SolicitudAyuda solicitud = solicitudAyudaRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // Validar que el estado sea ACEPTADA
        if (!"ACEPTADA".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud no se encuentra en curso (estado ACEPTADA)");
        }

        // Validar que el usuario que intenta terminar sea el discapacitado o el voluntario aceptado
        boolean isDiscapacitado = solicitud.getDiscapacitado() != null && usuarioId.equals(solicitud.getDiscapacitado().getId());
        boolean isVoluntario = solicitud.getVoluntarioAceptado() != null && usuarioId.equals(solicitud.getVoluntarioAceptado().getId());

        if (!isDiscapacitado && !isVoluntario) {
            throw new IllegalArgumentException("No estás autorizado para terminar esta ayuda");
        }

        // Cambiar el estado a FINALIZADA
        solicitud.setEstado("FINALIZADA");
        solicitudAyudaRepository.save(solicitud);

        // Crear registro en historial
        HistorialAyuda historial = new HistorialAyuda(solicitud);
        historialAyudaRepository.save(historial);

        // Notificar por WebSocket a ambos usuarios
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "AYUDA_FINALIZADA");
        payload.put("solicitudId", solicitudId);
        payload.put("mensaje", "La ayuda ha sido completada correctamente.");

        if (solicitud.getDiscapacitado() != null) {
            connectionRegistry.sendToUser(solicitud.getDiscapacitado().getId(), payload);
        }
        if (solicitud.getVoluntarioAceptado() != null) {
            connectionRegistry.sendToUser(solicitud.getVoluntarioAceptado().getId(), payload);
        }
    }

    public void calificarAyuda(Long historialId, String calificacion, Long usuarioId) {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado"));

        if (historial.getSolicitud().getDiscapacitado() == null ||
                !usuarioId.equals(historial.getSolicitud().getDiscapacitado().getId())) {
            throw new IllegalArgumentException("No estás autorizado para calificar esta ayuda");
        }

        List<String> validas = List.of("SIN_CALIFICAR", "MEDALLA_ORO", "MEDALLA_PLATA", "MEDALLA_COBRE");
        if (!validas.contains(calificacion)) {
            throw new IllegalArgumentException("Calificación no válida");
        }

        historial.setCalificacion(calificacion);
        historialAyudaRepository.save(historial);
    }

    public void guardarComentarioDiscapacitado(Long historialId, String comentario, Long usuarioId) {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado"));

        if (historial.getSolicitud().getDiscapacitado() == null ||
                !usuarioId.equals(historial.getSolicitud().getDiscapacitado().getId())) {
            throw new IllegalArgumentException("No estás autorizado para comentar esta ayuda");
        }

        historial.setComentarioDiscapacitado(comentario);
        historialAyudaRepository.save(historial);
    }

    public void guardarComentarioVoluntario(Long historialId, String comentario, Long usuarioId) {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado"));

        if (historial.getSolicitud().getVoluntarioAceptado() == null ||
                !usuarioId.equals(historial.getSolicitud().getVoluntarioAceptado().getId())) {
            throw new IllegalArgumentException("No estás autorizado para comentar esta ayuda");
        }

        historial.setComentarioVoluntario(comentario);
        historialAyudaRepository.save(historial);
    }

    public void guardarIncidenciaDiscapacitado(Long historialId, String descripcion, MultipartFile archivo, Long usuarioId) throws java.io.IOException {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado"));

        if (historial.getSolicitud().getDiscapacitado() == null ||
                !usuarioId.equals(historial.getSolicitud().getDiscapacitado().getId())) {
            throw new IllegalArgumentException("No estás autorizado para reportar incidencias en esta ayuda");
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setHistorialAyuda(historial);
        incidencia.setDenunciante(historial.getSolicitud().getDiscapacitado());
        incidencia.setDenunciado(historial.getSolicitud().getVoluntarioAceptado());
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado("PENDIENTE");

        if (archivo != null && !archivo.isEmpty()) {
            String path = guardarArchivoIncidencia(archivo, "incidencia_disca");
            incidencia.setEvidenciaUrl(path);
        }

        incidenciaRepository.save(incidencia);
    }

    public void guardarIncidenciaVoluntario(Long historialId, String descripcion, MultipartFile archivo, Long usuarioId) throws java.io.IOException {
        HistorialAyuda historial = historialAyudaRepository.findById(historialId)
                .orElseThrow(() -> new IllegalArgumentException("Historial de ayuda no encontrado"));

        if (historial.getSolicitud().getVoluntarioAceptado() == null ||
                !usuarioId.equals(historial.getSolicitud().getVoluntarioAceptado().getId())) {
            throw new IllegalArgumentException("No estás autorizado para reportar incidencias en esta ayuda");
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setHistorialAyuda(historial);
        incidencia.setDenunciante(historial.getSolicitud().getVoluntarioAceptado());
        incidencia.setDenunciado(historial.getSolicitud().getDiscapacitado());
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado("PENDIENTE");

        if (archivo != null && !archivo.isEmpty()) {
            String path = guardarArchivoIncidencia(archivo, "incidencia_vol");
            incidencia.setEvidenciaUrl(path);
        }

        incidenciaRepository.save(incidencia);
    }

    private String guardarArchivoIncidencia(MultipartFile file, String prefijo) throws java.io.IOException {
        String uploadDir = "uploads/incidencias/";
        java.io.File directorio = new java.io.File(uploadDir);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String nombreArchivo = prefijo + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        String rutaCompleta = uploadDir + nombreArchivo;

        java.nio.file.Path path = java.nio.file.Paths.get(rutaCompleta);
        java.nio.file.Files.write(path, file.getBytes());

        return "/uploads/incidencias/" + nombreArchivo;
    }
}
