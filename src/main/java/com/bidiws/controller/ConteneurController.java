package com.bidiws.controller;

import com.bidiws.dto.conteneur.ConteneurRequestDto;
import com.bidiws.dto.conteneur.ConteneurResponseDto;
import com.bidiws.service.ConteneurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conteneurs")
@RequiredArgsConstructor
public class ConteneurController {

    private final ConteneurService conteneurService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#dto.residenceId(), authentication) " +
            "or @authorizationService.isSyndicOfResidence(#dto.residenceId(), authentication) " +
            "or @authorizationService.isGardienOfResidence(#dto.residenceId(), authentication)")
    public ResponseEntity<ConteneurResponseDto> create(@Valid @RequestBody ConteneurRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conteneurService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessConteneur(#id, authentication)")
    public ResponseEntity<ConteneurResponseDto> update(@PathVariable Long id, @Valid @RequestBody ConteneurRequestDto dto) {
        return ResponseEntity.ok(conteneurService.update(id, dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessConteneur(#id, authentication)")
    public ResponseEntity<ConteneurResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conteneurService.getById(id));
    }

    @GetMapping("/residence/{residenceId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isSyndicOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isGardienOfResidence(#residenceId, authentication)")
    public ResponseEntity<List<ConteneurResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(conteneurService.getByResidence(residenceId));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("@authorizationService.canAccessConteneur(#id, authentication)")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        conteneurService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
