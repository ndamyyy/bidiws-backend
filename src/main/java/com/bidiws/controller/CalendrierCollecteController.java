package com.bidiws.controller;

import com.bidiws.dto.calendriercollecte.CalendrierCollecteRequestDto;
import com.bidiws.dto.calendriercollecte.CalendrierCollecteResponseDto;
import com.bidiws.service.CalendrierCollecteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calendriers-collecte")
@RequiredArgsConstructor
public class CalendrierCollecteController {

    private final CalendrierCollecteService calendrierCollecteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<CalendrierCollecteResponseDto> create(@Valid @RequestBody CalendrierCollecteRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendrierCollecteService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<CalendrierCollecteResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CalendrierCollecteRequestDto dto
    ) {
        return ResponseEntity.ok(calendrierCollecteService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalendrierCollecteResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(calendrierCollecteService.getById(id));
    }

    @GetMapping("/residence/{residenceId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isSyndicOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isGardienOfResidence(#residenceId, authentication)")
    public ResponseEntity<List<CalendrierCollecteResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(calendrierCollecteService.getByResidence(residenceId));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        calendrierCollecteService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
