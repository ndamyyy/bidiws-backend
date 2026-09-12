package com.bidiws.config;

import com.bidiws.security.JwtHandshakeInterceptor;
import com.bidiws.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * /topic  : diffusion large (ex: statut/position d'une tournee suivie par plusieurs abonnes)
 * /queue  : messages cibles a un utilisateur (ex: notification personnelle via convertAndSendToUser)
 * L'authentification et le scoping par tournee/residence sont geres par
 * StompAuthChannelInterceptor, pas ici.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    // List<String>, pas String : voir le meme commentaire dans SecurityConfig.
    // setAllowedOrigins(String...) traitait auparavant la valeur entiere
    // (virgules comprises) comme une seule origine, qui ne peut jamais
    // matcher un Origin reel envoye par le navigateur lors du handshake.
    @Value("${bidiws.websocket.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
