package com.bidiws.controller;

import com.bidiws.dto.residencehabitant.ResidenceHabitantRequestDto;
import com.bidiws.dto.residencehabitant.ResidenceHabitantResponseDto;
import com.bidiws.service.ResidenceHabitantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/residence-habitants")
@RequiredArgsConstructor
public class ResidenceHabitantController {

    private final ResidenceHabitantService residenceHabitantService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#dto.residenceId(), authentication) " +
            "or @authorizationService.isSyndicOfResidence(#dto.residenceId(), authentication) " +
            "or @authorizationService.isGardienOfResidence(#dto.residenceId(), authentication)")
    public ResponseEntity<ResidenceHabitantResponseDto> affecter(@Valid @RequestBody ResidenceHabitantRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residenceHabitantService.affecter(dto));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isSyndicOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isGardienOfResidence(#residenceId, authentication)")
    public ResponseEntity<Void> retirer(
            @RequestParam Long residenceId,
            @RequestParam Long habitantId
    ) {
        residenceHabitantService.retirer(residenceId, habitantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/residence/{residenceId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isMairieOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isSyndicOfResidence(#residenceId, authentication) " +
            "or @authorizationService.isGardienOfResidence(#residenceId, authentication)")
    public ResponseEntity<List<ResidenceHabitantResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(residenceHabitantService.getByResidence(residenceId));
    }

    @GetMapping("/habitant/{habitantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAIRIE') or @authorizationService.isSelf(#habitantId, authentication)")
    public ResponseEntity<List<ResidenceHabitantResponseDto>> getByHabitant(@PathVariable Long habitantId) {
        return ResponseEntity.ok(residenceHabitantService.getByHabitant(habitantId));
    }
}
