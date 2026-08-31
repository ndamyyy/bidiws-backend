package com.bidiws.controller;

import com.bidiws.dto.notification.NotificationRequestDto;
import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<NotificationResponseDto> create(@Valid @RequestBody NotificationRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(dto));
    }

    @PatchMapping("/{id}/lue")
    public ResponseEntity<NotificationResponseDto> marquerLue(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(notificationService.marquerLue(id, userDetails.getId()));
    }

    @PatchMapping("/{id}/envoyee")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<NotificationResponseDto> marquerEnvoyee(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.marquerEnvoyee(id));
    }

    @GetMapping("/destinataire/{destinataireId}")
    public ResponseEntity<List<NotificationResponseDto>> getByDestinataire(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long destinataireId,
            @RequestParam(required = false) Boolean nonLues
    ) {
        verifierDestinataire(userDetails, destinataireId);
        if (Boolean.TRUE.equals(nonLues)) {
            return ResponseEntity.ok(notificationService.getNonLues(destinataireId));
        }
        return ResponseEntity.ok(notificationService.getByDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/compteur")
    public ResponseEntity<Map<String, Long>> countNonLues(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long destinataireId
    ) {
        verifierDestinataire(userDetails, destinataireId);
        return ResponseEntity.ok(Map.of("nonLues", notificationService.countNonLues(destinataireId)));
    }

    private void verifierDestinataire(CustomUserDetails userDetails, Long destinataireId) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !userDetails.getId().equals(destinataireId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Accès non autorisé");
        }
    }
}
