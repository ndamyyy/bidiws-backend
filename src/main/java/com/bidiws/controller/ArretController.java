package com.bidiws.controller;

import com.bidiws.dto.arret.ArretIncidentRequestDto;
import com.bidiws.dto.arret.ArretRequestDto;
import com.bidiws.dto.arret.ArretResponseDto;
import com.bidiws.enums.StatutArret;
import com.bidiws.service.ArretService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/arrets")
@RequiredArgsConstructor
public class ArretController {

    private final ArretService arretService;

    @PostMapping
    public ResponseEntity<ArretResponseDto> creer(@Valid @RequestBody ArretRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arretService.creer(dto));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ArretResponseDto> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutArret statut
    ) {
        return ResponseEntity.ok(arretService.changerStatut(id, statut));
    }

    @PatchMapping("/{id}/incident")
    public ResponseEntity<ArretResponseDto> signalerIncident(
            @PathVariable Long id,
            @Valid @RequestBody ArretIncidentRequestDto dto
    ) {
        return ResponseEntity.ok(arretService.signalerIncident(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArretResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(arretService.getById(id));
    }

    @GetMapping("/tournee/{tourneeId}")
    public ResponseEntity<List<ArretResponseDto>> getByTournee(@PathVariable Long tourneeId) {
        return ResponseEntity.ok(arretService.getByTournee(tourneeId));
    }

    @GetMapping("/residence/{residenceId}")
    public ResponseEntity<List<ArretResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(arretService.getByResidence(residenceId));
    }
}