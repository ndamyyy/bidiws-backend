package com.bidiws.security;

import com.bidiws.entity.AppareilIot;
import com.bidiws.repository.AppareilIotRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authentifie un appareil IoT sur la chaine /iot/** : lit le header
 * X-Device-Api-Key, hash la cle recue (SHA-256) et cherche un
 * AppareilIot actif dont cle_api_hash correspond. Contrairement a
 * JwtAuthenticationFilter (Bearer + JWT), pas de compte utilisateur
 * implique — le principal pose dans le SecurityContext est directement
 * l'AppareilIot (cf. DeviceAuthenticationToken).
 */
@Component
@RequiredArgsConstructor
public class DeviceApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Device-Api-Key";

    private final AppareilIotRepository appareilIotRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String cle = extractCle(request);

        if (cle != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<AppareilIot> appareil = appareilIotRepository.findByCleApiHash(apiKeyHasher.hash(cle));

            if (appareil.isPresent() && Boolean.TRUE.equals(appareil.get().getActif())) {
                SecurityContextHolder.getContext().setAuthentication(new DeviceAuthenticationToken(appareil.get()));
            }
        }

        filterChain.doFilter(request, response);
    }

    @Nullable
    private String extractCle(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        return (header != null && !header.isBlank()) ? header : null;
    }
}
