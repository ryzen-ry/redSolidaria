package com.redsolidaria.enjambre.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redsolidaria.enjambre.service.AyudaService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AyudaWebSocketHandler extends TextWebSocketHandler {

    private final AyudaService ayudaService;
    private final AyudaConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    public static final String ATTR_USUARIO_ID = "usuarioId";
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
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (IOException ignored) {
            }
            return;
        }

        connectionRegistry.register(usuarioId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectionRegistry.unregister(session, status);
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
                if (!"DISCAPACITADO".equals(rol)) return;
                try {
                    var solicitud = ayudaService.solicitarAyuda(usuarioId);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "AYUDA_ENVIADA");
                    payload.put("solicitudId", solicitud.getId());
                    connectionRegistry.sendToUser(usuarioId, payload);
                } catch (IllegalArgumentException ex) {
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
                ayudaService.cancelarSolicitud(solicitudId, usuarioId);
            }
            case "RESPUESTA_AYUDA" -> {
                if (!"VOLUNTARIO".equals(rol)) return;
                long solicitudId = root.get("solicitudId").asLong();
                String decision = root.get("decision").asText();
                ayudaService.responderAyuda(solicitudId, usuarioId, decision);
            }
            case "TERMINAR_AYUDA" -> {
                Long solicitudId = root.has("solicitudId") && !root.get("solicitudId").isNull()
                        ? root.get("solicitudId").asLong()
                        : null;
                if (solicitudId != null) {
                    ayudaService.terminarAyuda(solicitudId, usuarioId);
                }
            }
            default -> {
            }
        }
    }
}

