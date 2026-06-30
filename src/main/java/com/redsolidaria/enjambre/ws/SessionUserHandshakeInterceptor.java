package com.redsolidaria.enjambre.ws;

import com.redsolidaria.enjambre.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.util.HashMap;
import java.util.Map;

public class SessionUserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        String query = request.getURI().getQuery();
        System.out.println("[WS Handshake] Request URI: " + request.getURI() + " | Query: " + query);
        
        // 1. Intentar obtener desde los parámetros de consulta de la URL (más confiable en Railway/producción)
        if (query != null && !query.isEmpty()) {
            Map<String, String> queryParams = parseQuery(query);
            String userIdStr = queryParams.get("usuarioId");
            String userRolStr = queryParams.get("usuarioRol");
            System.out.println("[WS Handshake] Extracted params - usuarioId: " + userIdStr + ", usuarioRol: " + userRolStr);
            if (userIdStr != null && !userIdStr.isEmpty() && !"null".equals(userIdStr) &&
                userRolStr != null && !userRolStr.isEmpty() && !"null".equals(userRolStr)) {
                try {
                    long uid = Long.parseLong(userIdStr);
                    attributes.put(AyudaWebSocketHandler.ATTR_USUARIO_ID, uid);
                    attributes.put(AyudaWebSocketHandler.ATTR_USUARIO_ROL, userRolStr);
                    System.out.println("[WS Handshake] Success via Query Params! usuarioId: " + uid + ", rol: " + userRolStr);
                    return true;
                } catch (NumberFormatException ignored) {
                    System.err.println("[WS Handshake] Failed to parse usuarioId as long: " + userIdStr);
                }
            }
        }

        // 2. Fallback: Intentar obtener desde HttpServletRequest HttpSession
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            HttpSession session = httpServletRequest.getSession(false);
            System.out.println("[WS Handshake] Fallback - HttpSession: " + (session != null ? "exists" : "null"));
            if (session != null) {
                Object usuarioId = session.getAttribute("usuarioId");
                Object usuarioRol = session.getAttribute("usuarioRol");
                System.out.println("[WS Handshake] Fallback - session.usuarioId: " + usuarioId + ", session.usuarioRol: " + usuarioRol);
                if (usuarioId instanceof Number idNum && usuarioRol instanceof String rolStr) {
                    attributes.put(AyudaWebSocketHandler.ATTR_USUARIO_ID, idNum.longValue());
                    attributes.put(AyudaWebSocketHandler.ATTR_USUARIO_ROL, rolStr);
                    System.out.println("[WS Handshake] Success via Session attributes! usuarioId: " + idNum.longValue() + ", rol: " + rolStr);
                    return true;
                }

                Object usuarioObj = session.getAttribute("usuario");
                System.out.println("[WS Handshake] Fallback - session.usuario: " + (usuarioObj != null ? "exists" : "null"));
                if (usuarioObj instanceof Usuario usuario) {
                    attributes.put(AyudaWebSocketHandler.ATTR_USUARIO_ID, usuario.getId());
                    attributes.put(AyudaWebSocketHandler.ATTR_USUARIO_ROL, usuario.getRol());
                    System.out.println("[WS Handshake] Success via Session Object! usuarioId: " + usuario.getId() + ", rol: " + usuario.getRol());
                    return true;
                }
            }
        }

        System.out.println("[WS Handshake] Completed with empty authentication attributes.");
        return true;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        
        String cleanQuery = query.replace("&amp;", "&");
        String[] pairs = cleanQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return map;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}


