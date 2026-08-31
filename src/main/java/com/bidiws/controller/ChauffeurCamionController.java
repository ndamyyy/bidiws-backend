package com.bidiws.controller;

import com.bidiws.dto.chauffeurcamion.ChauffeurCamionRequestDto;
import com.bidiws.dto.chauffeurcamion.ChauffeurCamionResponseDto;
import com.bidiws.service.ChauffeurCamionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chauffeur-camions")
@RequiredArgsConstructor
public class ChauffeurCamionController {

    private final ChauffeurCamionService chauffeurCamionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessCamion(#dto.camionId(), authentication)")
    public ResponseEntity<ChauffeurCamionResponseDto> affecter(@Valid @RequestBody ChauffeurCamionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chauffeurCamionService.affecter(dto));
    }

    @PatchMapping("/terminer")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessCamion(#camionId, authentication)")
    public ResponseEntity<ChauffeurCamionResponseDto> terminer(
            @RequestParam Long chauffeurId,
            @RequestParam Long camionId
    ) {
        return ResponseEntity.ok(chauffeurCamionService.terminer(chauffeurId, camionId));
    }

    @GetMapping("/chauffeur/{chauffeurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isSelf(#chauffeurId, authentication)")
    public ResponseEntity<List<ChauffeurCamionResponseDto>> getByChauffeur(@PathVariable Long chauffeurId) {
        return ResponseEntity.ok(chauffeurCamionService.getByChauffeur(chauffeurId));
    }

    @GetMapping("/camion/{camionId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessCamion(#camionId, authentication)")
    public ResponseEntity<List<ChauffeurCamionResponseDto>> getByCamion(@PathVariable Long camionId) {
        return ResponseEntity.ok(chauffeurCamionService.getByCamion(camionId));
    }
}
