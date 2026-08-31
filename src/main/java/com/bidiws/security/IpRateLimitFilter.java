package com.bidiws.security;

import com.bidiws.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

// Rate-limit par IP sur /auth/login et /auth/register, en complement du
// verrouillage par compte (AuthService.enregistrerEchec) : celui-ci ne
// protege pas contre le credential stuffing (1 mot de passe teste sur des
// milliers de comptes differents, aucun ne declenchant jamais son propre
// verrouillage), ni contre l'enumeration d'emails via /auth/register (409
// "compte deja existant" sans aucune limitation de debit).
//
// Compteur PARTAGE entre les deux routes (pas un par route) : rien
// n'empeche sinon de contourner la limite de l'une en pilonnant l'autre.
@Component
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> ROUTES_PROTEGEES = Set.of("/auth/login", "/auth/register");

    private final IpRateLimiter ipRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!ROUTES_PROTEGEES.contains(request.getServletPath()) || ipRateLimiter.autoriser(clientIp(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(IpRateLimiter.FENETRE.toSeconds()));

        ApiError body = new ApiError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Trop de tentatives depuis cette adresse IP, réessayez plus tard",
                Instant.now().toString(),
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // Pas de lecture de X-Forwarded-For : aucun reverse proxy de confiance
    // n'est configure devant cette app aujourd'hui (pas de
    // server.forward-headers-strategy) — un client pourrait sinon usurper
    // n'importe quelle IP via un simple header et contourner la limite.
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
