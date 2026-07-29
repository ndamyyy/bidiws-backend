package com.bidiws.controller;

import com.bidiws.dto.residence.ResidenceRequestDto;
import com.bidiws.dto.residence.ResidenceResponseDto;
import com.bidiws.service.ResidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/residences")
@RequiredArgsConstructor
public class ResidenceController {

    private final ResidenceService residenceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<ResidenceResponseDto> create(@Valid @RequestBody ResidenceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residenceService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<ResidenceResponseDto> update(
            @PathVariable long id,
            @Valid @RequestBody ResidenceRequestDto dto
    ) {
        return ResponseEntity.ok(residenceService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResidenceResponseDto> getById(@PathVariable long id) {
        return ResponseEntity.ok(residenceService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ResidenceResponseDto>> getAll(
            @RequestParam(required = false) Long villeId,
            @RequestParam(required = false) Long zoneId
    ) {
        if (villeId != null) {
            return ResponseEntity.ok(residenceService.getByVille(villeId));
        }
        if (zoneId != null) {
            return ResponseEntity.ok(residenceService.getByZone(zoneId));
        }
        return ResponseEntity.ok(residenceService.getAll());
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        residenceService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
