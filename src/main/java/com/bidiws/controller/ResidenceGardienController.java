package com.bidiws.controller;

import com.bidiws.dto.residencegardien.ResidenceGardienRequestDto;
import com.bidiws.dto.residencegardien.ResidenceGardienResponseDto;
import com.bidiws.service.ResidenceGardienService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/residence-gardiens")
@RequiredArgsConstructor
public class ResidenceGardienController {

    private final ResidenceGardienService residenceGardienService;

    @PostMapping
    public ResponseEntity<ResidenceGardienResponseDto> affecter(@Valid @RequestBody ResidenceGardienRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residenceGardienService.affecter(dto));
    }

    @DeleteMapping
    public ResponseEntity<Void> retirer(
            @RequestParam Long residenceId,
            @RequestParam Long gardienId
    ) {
        residenceGardienService.retirer(residenceId, gardienId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/residence/{residenceId}")
    public ResponseEntity<List<ResidenceGardienResponseDto>> getByResidence(@PathVariable Long residenceId) {
        return ResponseEntity.ok(residenceGardienService.getByResidence(residenceId));
    }

    @GetMapping("/gardien/{gardienId}")
    public ResponseEntity<List<ResidenceGardienResponseDto>> getByGardien(@PathVariable Long gardienId) {
        return ResponseEntity.ok(residenceGardienService.getByGardien(gardienId));
    }
}