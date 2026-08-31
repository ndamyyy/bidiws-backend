package com.bidiws.security;

import com.bidiws.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DeviceApiKeyAuthenticationFilter deviceApiKeyAuthenticationFilter;
    private final ObjectMapper objectMapper;

    // List<String>, pas String : bidiws.websocket.allowed-origins contient
    // plusieurs origines separees par des virgules. Spring convertit
    // nativement une propriete a virgules en List<String> pour un champ
    // @Value ainsi type — mal type en String, la valeur entiere (virgules
    // comprises) finissait comme un unique element de List.of(...), qui ne
    // peut matcher aucun Origin reel envoye par un navigateur.
    @Value("${bidiws.websocket.allowed-origins}")
    private List<String> allowedOrigins;

    private static final String[] PUBLIC_ROUTES = {
            "/auth/login",
            "/auth/register",
            "/error",
            "/ws/**"
    };

    // Chaine dediee aux appareils IoT : cle API (X-Device-Api-Key), pas de
    // JWT. Isolee du reste — si une cle fuit un jour, ca ne touche que ce
    // perimetre, jamais l'authentification utilisateur. @Order(1) : evaluee
    // avant la chaine principale, qui ne voit donc jamais /iot/**.
    @Bean
    @Order(1)
    public SecurityFilterChain iotFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/iot/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleUnauthorized)
                        .accessDeniedHandler(this::handleForbidden)
                )
                .addFilterBefore(deviceApiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleUnauthorized)
                        .accessDeniedHandler(this::handleForbidden)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Réponses d'erreur au format ApiError attendu par le frontend ──
    // { status, message, timestamp, path }

    private void handleUnauthorized(jakarta.servlet.http.HttpServletRequest request,
                                     HttpServletResponse response,
                                     org.springframework.security.core.AuthenticationException ex) throws java.io.IOException {
        writeError(response, request.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise ou token invalide");
    }

    private void handleForbidden(jakarta.servlet.http.HttpServletRequest request,
                                  HttpServletResponse response,
                                  org.springframework.security.access.AccessDeniedException ex) throws java.io.IOException {
        writeError(response, request.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Accès refusé pour ce rôle");
    }

    private void writeError(HttpServletResponse response, String path, int status, String message) throws java.io.IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        ApiError body = new ApiError(status, message, Instant.now().toString(), path);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
