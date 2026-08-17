package com.bidiws.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status).body(new ApiError(status, ex.getReason(), Instant.now().toString(), request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(HttpStatus.BAD_REQUEST.value(), message, Instant.now().toString(), request.getRequestURI())
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiError(HttpStatus.UNAUTHORIZED.value(), "Email ou mot de passe incorrect", Instant.now().toString(), request.getRequestURI())
        );
    }

    // Couvre AccessDeniedException ET sa sous-classe AuthorizationDeniedException
    // (levee par @PreAuthorize depuis Spring Security 6+) : sans ce handler,
    // TOUT refus d'autorisation remontait en 500 generique au lieu d'un 403
    // propre — pas une faille de securite en soi (l'acces restait bloque),
    // mais un mauvais code HTTP et un message trompeur pour le client.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiError(HttpStatus.FORBIDDEN.value(), "Accès non autorisé", Instant.now().toString(), request.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erreur non geree sur {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne du serveur", Instant.now().toString(), request.getRequestURI())
        );
    }

}
