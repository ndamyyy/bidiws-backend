package com.bidiws.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Recupere le JWT passe en query param (?token=...) au moment du handshake
 * SockJS/WebSocket et le range dans les attributs de session STOMP. Sert de
 * repli quand le client ne peut pas poser un header STOMP "Authorization"
 * sur la frame CONNECT (ex: transport SockJS xhr-streaming).
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    static final String TOKEN_ATTRIBUTE = "token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = httpRequest.getParameter("token");
            if (token != null && !token.isBlank()) {
                attributes.put(TOKEN_ATTRIBUTE, token);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
