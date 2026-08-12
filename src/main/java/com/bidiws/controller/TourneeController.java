package com.bidiws.controller;

import com.bidiws.dto.tournee.TourneeRequestDto;
import com.bidiws.dto.tournee.TourneeResponseDto;
import com.bidiws.service.TourneeService;
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
@RequestMapping("/tournees")
@RequiredArgsConstructor
public class TourneeController {

    private final TourneeService tourneeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<TourneeResponseDto> create(@Valid @RequestBody TourneeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourneeService.create(dto));
    }

    @PatchMapping("/{id}/demarrer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isAssignedChauffeur(#id, authentication)")
    public ResponseEntity<TourneeResponseDto> demarrer(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.demarrer(id));
    }

    @PatchMapping("/{id}/terminer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isAssignedChauffeur(#id, authentication)")
    public ResponseEntity<TourneeResponseDto> terminer(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.terminer(id));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<TourneeResponseDto> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.annuler(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessTournee(#id, authentication)")
    public ResponseEntity<TourneeResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') " +
            "or (#chauffeurId != null and @authorizationService.isSelf(#chauffeurId, authentication))")
    public ResponseEntity<List<TourneeResponseDto>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long chauffeurId
    ) {
        if (date != null) {
            return ResponseEntity.ok(tourneeService.getByDate(date));
        }
        if (chauffeurId != null) {
            return ResponseEntity.ok(tourneeService.getByChauffeur(chauffeurId));
        }
        return ResponseEntity.ok(tourneeService.getAll());
    }
}
