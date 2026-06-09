package com.redsolidaria.enjambre.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        }
    }

    public void sendToUser(Long usuarioId, Map<String, Object> payload) {
        Map<String, WebSocketSession> sesiones = sesionesPorUsuario.get(usuarioId);
        if (sesiones == null || sesiones.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sesiones.values()) {
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            // Bug #4 fix: loggear el error para diagnóstico en lugar de ignorarlo silenciosamente.
            System.err.println("[WS] Error al enviar mensaje a usuario " + usuarioId
                    + ": " + e.getMessage());
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

