package com.bidiws.controller;

import com.bidiws.dto.calendriercollecte.CalendrierCollecteRequestDto;
import com.bidiws.dto.calendriercollecte.CalendrierCollecteResponseDto;
import com.bidiws.dto.calendriercollecte.ResidenceADesservirDto;
import com.bidiws.service.CalendrierCollecteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    @PreAuthorize("@authorizationService.canAccessCalendrier(#id, authentication)")
    public ResponseEntity<CalendrierCollecteResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(calendrierCollecteService.getById(id));
    }

    @GetMapping("/residence/{residenceId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isSyndicOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isGardienOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isHabitantOfResidence(#residenceId, authentication)")
    public ResponseEntity<List<CalendrierCollecteResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(calendrierCollecteService.getByResidence(residenceId));
    }

    // Suggestion des résidences à desservir un jour donné, dans une zone
    // donnée, pour un type de collecte donné — déduite du calendrier
    // récurrent. Lecture seule (aucune création d'arrêt/tournée) : le
    // frontend propose la liste à cocher/décocher avant toute création
    // groupée, jamais une automatisation aveugle.
    @GetMapping("/residences-a-desservir")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<List<ResidenceADesservirDto>> getResidencesADesservir(
            @RequestParam Long zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long typeCollecteId
    ) {
        return ResponseEntity.ok(calendrierCollecteService.getResidencesADesservir(zoneId, date, typeCollecteId));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        calendrierCollecteService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
