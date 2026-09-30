package com.bidiws.controller;

import com.bidiws.dto.residencesyndic.ResidenceSyndicRequestDto;
import com.bidiws.dto.residencesyndic.ResidenceSyndicResponseDto;
import com.bidiws.service.ResidenceSyndicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/residence-syndics")
@RequiredArgsConstructor
public class ResidenceSyndicController {

    private final ResidenceSyndicService residenceSyndicService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#dto.residenceId(), authentication)")
    public ResponseEntity<ResidenceSyndicResponseDto> affecter(@Valid @RequestBody ResidenceSyndicRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residenceSyndicService.affecter(dto));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication)")
    public ResponseEntity<Void> retirer(
            @RequestParam Long residenceId,
            @RequestParam Long syndicId
    ) {
        residenceSyndicService.retirer(residenceId, syndicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/residence/{residenceId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isSyndicOfResidence(#residenceId, authentication)")
    public ResponseEntity<List<ResidenceSyndicResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(residenceSyndicService.getByResidence(residenceId));
    }

    @GetMapping("/syndic/{syndicId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelf(#syndicId, authentication)")
    public ResponseEntity<List<ResidenceSyndicResponseDto>> getBySyndic(@PathVariable Long syndicId) {
        return ResponseEntity.ok(residenceSyndicService.getBySyndic(syndicId));
    }
}
