package com.bidiws.controller;

import com.bidiws.dto.signalgps.SignalGpsRequestDto;
import com.bidiws.dto.signalgps.SignalGpsResponseDto;
import com.bidiws.service.SignalGpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/signaux-gps")
@RequiredArgsConstructor
public class SignalGpsController {

    private final SignalGpsService signalGpsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isAssignedChauffeur(#dto.tourneeId(), authentication)")
    public ResponseEntity<SignalGpsResponseDto> enregistrer(@Valid @RequestBody SignalGpsRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signalGpsService.enregistrer(dto));
    }

    @GetMapping("/tournee/{tourneeId}/dernier")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isAssignedChauffeur(#tourneeId, authentication)")
    public ResponseEntity<SignalGpsResponseDto> getDernierSignal(@PathVariable Long tourneeId) {
        return ResponseEntity.ok(signalGpsService.getDernierSignal(tourneeId));
    }

    @GetMapping("/tournee/{tourneeId}/trajet")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isAssignedChauffeur(#tourneeId, authentication)")
    public ResponseEntity<Page<SignalGpsResponseDto>> getTrajet(
            @PathVariable Long tourneeId,
            @PageableDefault(size = 200, sort = "horodatage", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(signalGpsService.getTrajet(tourneeId, pageable));
    }

    @GetMapping("/tournee/{tourneeId}/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isAssignedChauffeur(#tourneeId, authentication)")
    public ResponseEntity<Page<SignalGpsResponseDto>> getEntreDates(
            @PathVariable Long tourneeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 200, sort = "horodatage", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(signalGpsService.getEntreDates(tourneeId, debut, fin, pageable));
    }
}
