package com.bidiws.controller;

import com.bidiws.dto.signalement.SignalementRequestDto;
import com.bidiws.dto.signalement.SignalementResponseDto;
import com.bidiws.enums.StatutSignalement;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.service.SignalementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/signalements")
@RequiredArgsConstructor
public class SignalementController {

    private final SignalementService signalementService;

    @PostMapping
    public ResponseEntity<SignalementResponseDto> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SignalementRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(signalementService.create(userDetails.getId(), dto));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<SignalementResponseDto> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutSignalement statut
    ) {
        return ResponseEntity.ok(signalementService.changerStatut(id, statut));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SignalementResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(signalementService.getById(id));
    }

    @GetMapping("/auteur/{auteurId}")
    public ResponseEntity<List<SignalementResponseDto>> getByAuteur(@PathVariable Long auteurId) {
        return ResponseEntity.ok(signalementService.getByAuteur(auteurId));
    }

    @GetMapping
    public ResponseEntity<List<SignalementResponseDto>> getByStatut(@RequestParam StatutSignalement statut) {
        return ResponseEntity.ok(signalementService.getByStatut(statut));
    }
}