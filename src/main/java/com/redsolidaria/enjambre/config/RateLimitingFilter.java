package com.redsolidaria.enjambre.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Filtro de rate limiting para proteger endpoints de autenticación
 * contra ataques de fuerza bruta.
 * 
 * Límites:
 * - /api/auth/login: 5 intentos por minuto por IP
 * - /login (POST):   5 intentos por minuto por IP
 * - /api/auth/registro: 3 intentos por minuto por IP
 * - Otros:           20 requests por minuto por IP
 * 
 * Diferencia entre respuestas HTML y JSON según el tipo de petición:
 * - Peticiones AJAX/API (Accept: application/json o X-Requested-With: XMLHttpRequest)
 *   → Responden con JSON
 * - Peticiones de formulario HTML (navegador)
 *   → Redirigen a la página correspondiente con mensaje de error
 */
@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
    
    private static final long WINDOW_SIZE_MS = TimeUnit.MINUTES.toMillis(1);
    
    // Límites específicos por patrón de URL
    // Todos los endpoints de autenticación: 4 intentos por minuto
    // /api/auth/check-email: 20 intentos por minuto (usa DEFAULT_LIMIT)
    private static final int LOGIN_LIMIT = 4;
    private static final int REGISTER_LIMIT = 4;
    private static final int DEFAULT_LIMIT = 20;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIp(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Solo aplicar rate limiting a endpoints específicos
        if (!isRateLimitedEndpoint(requestUri, method)) {
            chain.doFilter(request, response);
            return;
        }
        
        String key = clientIp + ":" + requestUri;
        long now = System.currentTimeMillis();
        
        RateLimitEntry entry = requestCounts.compute(key, (k, v) -> {
            if (v == null || (now - v.windowStart) > WINDOW_SIZE_MS) {
                return new RateLimitEntry(now, 1);
            }
            v.count++;
            return v;
        });
        
        int limit = getLimitForEndpoint(requestUri);
        
        // Agregar cabeceras de rate limiting
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - entry.count)));
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf((entry.windowStart + WINDOW_SIZE_MS) / 1000));
        
        if (entry.count > limit) {
            // Determinar si la petición espera JSON (API/AJAX) o HTML (formulario de navegador)
            if (isAjaxRequest(httpRequest)) {
                // Respuesta JSON para APIs y peticiones AJAX
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(
                    "{\"error\":\"❌ Demasiados intentos. Espera 1 minuto antes de volver a intentarlo.\"}"
                );
            } else {
                // Respuesta HTML directa para formularios enviados desde el navegador.
                // No usamos redirect porque páginas como verificar-codigo dependen de atributos de sesión.
                httpResponse.setStatus(429);
                httpResponse.setContentType("text/html;charset=UTF-8");
                String redirectUrl = getRedirectUrlForEndpoint(requestUri);
                httpResponse.getWriter().write(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<title>Demasiados intentos</title>" +
                    "<style>" +
                    "body{font-family:'Inter',sans-serif;background:linear-gradient(135deg,#DBEAFE,#F0F4F8);min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0}" +
                    ".card{background:white;border-radius:24px;padding:48px;box-shadow:0 20px 40px rgba(0,0,0,0.1);max-width:420px;width:90%;text-align:center}" +
                    ".icon{font-size:3rem;margin-bottom:16px}" +
                    "h2{color:#991B1B;font-size:1.3rem;margin-bottom:12px}" +
                    "p{color:#4B5563;font-size:0.9rem;margin-bottom:24px;line-height:1.6}" +
                    ".btn{display:inline-block;background:#2563EB;color:white;padding:12px 28px;border-radius:40px;text-decoration:none;font-weight:600}" +
                    ".btn:hover{background:#1D4ED8}" +
                    "</style></head><body>" +
                    "<div class='card'>" +
                    "<div class='icon'>⏳</div>" +
                    "<h2>Demasiados intentos</h2>" +
                    "<p>Has superado el límite de intentos permitidos.<br><strong>Espera 1 minuto</strong> antes de volver a intentarlo.</p>" +
                    "<a href='" + redirectUrl + "' class='btn'>Volver al inicio</a>" +
                    "</div></body></html>"
                );
            }
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * Determina si la petición es AJAX/API o un formulario HTML de navegador.
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        // Si la URL empieza con /api/ es claramente una petición API
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return true;
        }
        
        // Si tiene el header X-Requested-With: XMLHttpRequest (JavaScript/fetch/AJAX)
        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith != null && "XMLHttpRequest".equalsIgnoreCase(xRequestedWith)) {
            return true;
        }
        
        // Si el Accept header pide explícitamente JSON
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Determina la URL de redirección según el endpoint que fue rate-limited.
     */
    private String getRedirectUrlForEndpoint(String uri) {
        if (uri == null) return "/login?error=rate_limit";
        
        if (uri.equals("/login")) {
            return "/login?error=rate_limit";
        }
        if (uri.contains("/registro/voluntario")) {
            return "/registro/voluntario?error=rate_limit";
        }
        if (uri.contains("/registro/discapacitado")) {
            return "/registro/discapacitado?error=rate_limit";
        }
        if (uri.contains("/verificar-codigo")) {
            return "/verificar-codigo?error=rate_limit";
        }
        
        return "/login?error=rate_limit";
    }
    
    private boolean isRateLimitedEndpoint(String uri, String method) {
        if (uri == null) return false;
        
        // Login endpoints
        if (uri.equals("/api/auth/login") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.equals("/login") && "POST".equalsIgnoreCase(method)) return true;
        
        // Registro endpoints
        if (uri.equals("/api/auth/registro/voluntario") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.equals("/api/auth/registro/discapacitado") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.equals("/registro/voluntario") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.equals("/registro/discapacitado") && "POST".equalsIgnoreCase(method)) return true;
        
        // Verificación de código
        if (uri.equals("/api/auth/verificar-codigo") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.equals("/verificar-codigo") && "POST".equalsIgnoreCase(method)) return true;
        
        // Check email (evitar enumeración rápida)
        if (uri.equals("/api/auth/check-email") && "GET".equalsIgnoreCase(method)) return true;
        
        return false;
    }
    
    private int getLimitForEndpoint(String uri) {
        if (uri == null) return DEFAULT_LIMIT;
        
        if (uri.contains("/login")) return LOGIN_LIMIT;
        if (uri.contains("/registro")) return REGISTER_LIMIT;
        if (uri.contains("/verificar-codigo")) return LOGIN_LIMIT;
        
        return DEFAULT_LIMIT;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private static class RateLimitEntry {
        final long windowStart;
        int count;
        
        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
