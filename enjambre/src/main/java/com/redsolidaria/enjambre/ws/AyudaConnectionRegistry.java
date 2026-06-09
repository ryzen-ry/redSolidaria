package com.redsolidaria.enjambre.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro centralizado de conexiones WebSocket activas.
 *
 * <p>Permite mapear un {@code usuarioId} a sus sesiones WebSocket abiertas
 * (puede haber más de una si el usuario tiene varias pestañas abiertas).
 * Usa {@link ConcurrentHashMap} para soporte multi-hilo sin bloqueos explícitos.
 *
 * <p>Usa <b>Logback (SLF4J)</b> via {@code @Slf4j} para registrar conexiones,
 * desconexiones y errores de envío.
 */
@Slf4j
@Component
public class AyudaConnectionRegistry {

    private final Map<Long, Map<String, WebSocketSession>> sesionesPorUsuario = new ConcurrentHashMap<>();
    private final Map<String, Long> usuarioPorSessionId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AyudaConnectionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long usuarioId, WebSocketSession session) {
        if (usuarioId == null || session == null) return;
        sesionesPorUsuario
                .computeIfAbsent(usuarioId, ignored -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        usuarioPorSessionId.put(session.getId(), usuarioId);
        log.debug("[WS] Registrada sesión {} para usuario {}", session.getId(), usuarioId);
    }

    public void unregister(WebSocketSession session, CloseStatus status) {
        if (session == null) return;
        Long usuarioId = usuarioPorSessionId.remove(session.getId());
        if (usuarioId != null) {
            Map<String, WebSocketSession> sesiones = sesionesPorUsuario.get(usuarioId);
            if (sesiones != null) {
                sesiones.remove(session.getId());
                if (sesiones.isEmpty()) {
                    sesionesPorUsuario.remove(usuarioId);
                }
            }
            log.debug("[WS] Sesión {} desregistrada para usuario {} (status={})",
                    session.getId(), usuarioId, status);
        }
    }

    public void sendToUser(Long usuarioId, Map<String, Object> payload) {
        Map<String, WebSocketSession> sesiones = sesionesPorUsuario.get(usuarioId);
        if (sesiones == null || sesiones.isEmpty()) {
            log.debug("[WS] sendToUser: usuario {} no está conectado, mensaje descartado.", usuarioId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sesiones.values()) {
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            // Bug #4 fix: log estructurado en lugar de System.err
            log.error("[WS] Error al enviar mensaje a usuario {}: {}", usuarioId, e.getMessage(), e);
        }
    }

    public boolean isUserConnected(Long usuarioId) {
        if (usuarioId == null) return false;
        Map<String, WebSocketSession> sesiones = sesionesPorUsuario.get(usuarioId);
        if (sesiones == null || sesiones.isEmpty()) return false;
        return sesiones.values().stream().anyMatch(s -> s != null && s.isOpen());
    }

    public Collection<WebSocketSession> getUserSessions(Long usuarioId) {
        Map<String, WebSocketSession> sesiones = sesionesPorUsuario.get(usuarioId);
        if (sesiones == null) return Collections.emptyList();
        return sesiones.values();
    }
}
