package com.bidiws.controller;

import com.bidiws.dto.notification.NotificationRequestDto;
import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDto> create(@Valid @RequestBody NotificationRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(dto));
    }

    @PatchMapping("/{id}/lue")
    public ResponseEntity<NotificationResponseDto> marquerLue(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.marquerLue(id));
    }

    @PatchMapping("/{id}/envoyee")
    public ResponseEntity<NotificationResponseDto> marquerEnvoyee(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.marquerEnvoyee(id));
    }

    @GetMapping("/destinataire/{destinataireId}")
    public ResponseEntity<List<NotificationResponseDto>> getByDestinataire(
            @PathVariable Long destinataireId,
            @RequestParam(required = false) Boolean nonLues
    ) {
        if (Boolean.TRUE.equals(nonLues)) {
            return ResponseEntity.ok(notificationService.getNonLues(destinataireId));
        }
        return ResponseEntity.ok(notificationService.getByDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/compteur")
    public ResponseEntity<Map<String, Long>> countNonLues(@PathVariable Long destinataireId) {
        return ResponseEntity.ok(Map.of("nonLues", notificationService.countNonLues(destinataireId)));
    }
}