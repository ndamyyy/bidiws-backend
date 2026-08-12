package com.bidiws.controller;

import com.bidiws.dto.residence.ResidenceRequestDto;
import com.bidiws.dto.residence.ResidenceResponseDto;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.service.ResidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/residences")
@RequiredArgsConstructor
public class ResidenceController {

    private final ResidenceService residenceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<ResidenceResponseDto> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ResidenceRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residenceService.create(dto, userDetails));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE', 'SYNDIC')")
    public ResponseEntity<ResidenceResponseDto> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id,
            @Valid @RequestBody ResidenceRequestDto dto
    ) {
        return ResponseEntity.ok(residenceService.update(id, dto, userDetails));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE', 'SYNDIC', 'GARDIEN')")
    public ResponseEntity<ResidenceResponseDto> getById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(residenceService.getById(id, userDetails));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE', 'SYNDIC', 'GARDIEN')")
    public ResponseEntity<List<ResidenceResponseDto>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long villeId,
            @RequestParam(required = false) Long zoneId
    ) {
        return ResponseEntity.ok(residenceService.getAll(userDetails, villeId, zoneId));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<Void> desactiver(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        residenceService.desactiver(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
