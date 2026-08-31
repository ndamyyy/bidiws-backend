package com.bidiws.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authentifie la frame STOMP CONNECT via le meme JWT que le REST (header
 * "Authorization", ou en repli le token de handshake pose par
 * JwtHandshakeInterceptor), et autorise les SUBSCRIBE sur les topics par
 * tournee selon le rattachement residence/ville de l'utilisateur.
 *
 * /ws/** est permitAll() au niveau HTTP (SecurityConfig) : c'est cet
 * intercepteur, pas le filtre servlet JWT, qui protege le canal STOMP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern TOURNEE_TOPIC_PATTERN =
            Pattern.compile("^/topic/tournees/(\\d+)(?:/position)?$");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthorizationService authorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticateConnect(accessor);
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            default -> { /* rien a verifier pour les autres frames */ }
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null) {
            throw new StompAuthenticationException("Token JWT manquant");
        }

        try {
            Long userId = jwtService.extractUserId(token);
            CustomUserDetails userDetails = userDetailsService.loadUserById(userId);

            if (!userDetails.isEnabled() || !jwtService.isTokenValid(token, userDetails)) {
                throw new StompAuthenticationException("Token JWT invalide");
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
        } catch (StompAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new StompAuthenticationException("Authentification WebSocket refusee");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = TOURNEE_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long tourneeId = Long.valueOf(matcher.group(1));

        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authorizationService.canAccessTournee(tourneeId, authentication)) {
            log.warn("Abonnement refuse a {} pour {}", destination, accessor.getUser());
            throw new StompAuthenticationException("Acces non autorise a cette tournee");
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            return header.startsWith(BEARER_PREFIX) ? header.substring(BEARER_PREFIX.length()) : header;
        }

        Object sessionToken = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get(JwtHandshakeInterceptor.TOKEN_ATTRIBUTE)
                : null;
        return sessionToken != null ? sessionToken.toString() : null;
    }
}
