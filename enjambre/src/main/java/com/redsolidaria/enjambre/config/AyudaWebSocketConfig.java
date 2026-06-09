package com.redsolidaria.enjambre.config;

import com.redsolidaria.enjambre.ws.AyudaWebSocketHandler;
import com.redsolidaria.enjambre.ws.SessionUserHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class AyudaWebSocketConfig implements WebSocketConfigurer {

    private final AyudaWebSocketHandler ayudaWebSocketHandler;

    public AyudaWebSocketConfig(AyudaWebSocketHandler ayudaWebSocketHandler) {
        this.ayudaWebSocketHandler = ayudaWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ayudaWebSocketHandler, "/ws/ayuda")
                .addInterceptors(new SessionUserHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}

