package com.redsolidaria.enjambre.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redsolidaria.enjambre.service.AyudaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler WebSocket para el flujo de ayuda de emergencia.
 *
 * <p>Procesa mensajes entrantes desde el cliente (discapacitado y voluntario)
 * y los delega al {@link AyudaService}. Usa <b>Logback (SLF4J)</b> via {@code @Slf4j}
 * para registrar cada tipo de evento recibido y errores de procesamiento.
 */
@Slf4j
@Component
public class AyudaWebSocketHandler extends TextWebSocketHandler {

    private final AyudaService ayudaService;
    private final AyudaConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    public static final String ATTR_USUARIO_ID  = "usuarioId";
    public static final String ATTR_USUARIO_ROL = "usuarioRol";

    public AyudaWebSocketHandler(
            AyudaService ayudaService,
            AyudaConnectionRegistry connectionRegistry,
            ObjectMapper objectMapper
    ) {
        this.ayudaService = ayudaService;
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Object usuarioIdObj = attrs.get(ATTR_USUARIO_ID);
        Long usuarioId = usuarioIdObj instanceof Number num ? num.longValue() : null;
        if (usuarioId == null) {
            log.warn("[WS] Conexión rechazada: falta atributo usuarioId en sesión {}", session.getId());
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (IOException ignored) {
            }
            return;
        }

        connectionRegistry.register(usuarioId, session);
        log.info("[WS] Conexión establecida — usuario={} sesión={}", usuarioId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectionRegistry.unregister(session, status);
        log.info("[WS] Conexión cerrada — sesión={} status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> attrs = session.getAttributes();
        Object usuarioIdObj = attrs.get(ATTR_USUARIO_ID);
        Long usuarioId = usuarioIdObj instanceof Number num ? num.longValue() : null;
        if (usuarioId == null) return;

        Object usuarioRolObj = attrs.get(ATTR_USUARIO_ROL);
        String rol = usuarioRolObj instanceof String ? (String) usuarioRolObj : null;

        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.has("type") ? root.get("type").asText() : null;
        if (type == null) return;

        log.debug("[WS] Mensaje recibido — type={} usuario={} rol={}", type, usuarioId, rol);

        switch (type) {
            case "LOCATION_UPDATE" -> {
                double lat = root.get("lat").asDouble();
                double lng = root.get("lng").asDouble();
                Double precision = root.has("precisionMetros") && !root.get("precisionMetros").isNull()
                        ? root.get("precisionMetros").asDouble()
                        : null;
                ayudaService.actualizarUbicacion(usuarioId, lat, lng, precision);
            }
            case "AYUDA_SOLICITADA" -> {
                if (!"DISCAPACITADO".equals(rol)) {
                    log.warn("[WS] AYUDA_SOLICITADA ignorada: rol={} no es DISCAPACITADO", rol);
                    return;
                }
                try {
                    var solicitud = ayudaService.solicitarAyuda(usuarioId);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "AYUDA_ENVIADA");
                    payload.put("solicitudId", solicitud.getId());
                    connectionRegistry.sendToUser(usuarioId, payload);
                } catch (IllegalArgumentException ex) {
                    log.warn("[WS] Error al solicitar ayuda para usuario {}: {}", usuarioId, ex.getMessage());
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "SOLICITUD_CANCELADA");
                    payload.put("mensaje", ex.getMessage());
                    connectionRegistry.sendToUser(usuarioId, payload);
                }
            }
            case "CANCELAR_SOLICITUD" -> {
                if (!"DISCAPACITADO".equals(rol)) return;
                Long solicitudId = root.has("solicitudId") && !root.get("solicitudId").isNull()
                        ? root.get("solicitudId").asLong()
                        : null;
                log.debug("[WS] CANCELAR_SOLICITUD — solicitudId={} usuario={}", solicitudId, usuarioId);
                ayudaService.cancelarSolicitud(solicitudId, usuarioId);
            }
            case "RESPUESTA_AYUDA" -> {
                if (!"VOLUNTARIO".equals(rol)) {
                    log.warn("[WS] RESPUESTA_AYUDA ignorada: rol={} no es VOLUNTARIO", rol);
                    return;
                }
                long solicitudId = root.get("solicitudId").asLong();
                String decision  = root.get("decision").asText();
                log.debug("[WS] RESPUESTA_AYUDA — solicitudId={} voluntario={} decision={}", solicitudId, usuarioId, decision);
                ayudaService.responderAyuda(solicitudId, usuarioId, decision);
            }
            case "TERMINAR_AYUDA" -> {
                Long solicitudId = root.has("solicitudId") && !root.get("solicitudId").isNull()
                        ? root.get("solicitudId").asLong()
                        : null;
                if (solicitudId != null) {
                    log.debug("[WS] TERMINAR_AYUDA — solicitudId={} usuario={}", solicitudId, usuarioId);
                    ayudaService.terminarAyuda(solicitudId, usuarioId);
                }
            }
            default -> log.debug("[WS] Tipo de mensaje desconocido '{}' de usuario={}", type, usuarioId);
        }
    }
}
