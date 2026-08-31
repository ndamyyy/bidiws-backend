package com.bidiws.controller;

import com.bidiws.dto.camion.CamionRequestDto;
import com.bidiws.dto.camion.CamionResponseDto;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.service.CamionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/camions")
@RequiredArgsConstructor
public class CamionController {

    private final CamionService camionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<CamionResponseDto> create(
            @Valid @RequestBody CamionRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(camionService.create(dto, userDetails));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessCamion(#id, authentication)")
    public ResponseEntity<CamionResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CamionRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(camionService.update(id, dto, userDetails));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessCamion(#id, authentication)")
    public ResponseEntity<CamionResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(camionService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE')")
    public ResponseEntity<List<CamionResponseDto>> getAll(
            @RequestParam(required = false) Boolean actif,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(camionService.getAll(userDetails, actif));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessCamion(#id, authentication)")
    public ResponseEntity<Void> desactiver(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        camionService.desactiver(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
