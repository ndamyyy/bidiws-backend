package com.bidiws.controller;

import com.bidiws.dto.camion.CamionRequestDto;
import com.bidiws.dto.camion.CamionResponseDto;
import com.bidiws.service.CamionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/camions")
@RequiredArgsConstructor
public class CamionController {

    private final CamionService camionService;

    @PostMapping
    public ResponseEntity<CamionResponseDto> create(@Valid @RequestBody CamionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(camionService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CamionResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CamionRequestDto dto
    ) {
        return ResponseEntity.ok(camionService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CamionResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(camionService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<CamionResponseDto>> getAll(
            @RequestParam(required = false) Boolean actif
    ) {
        if (Boolean.TRUE.equals(actif)) {
            return ResponseEntity.ok(camionService.getActifs());
        }
        return ResponseEntity.ok(camionService.getAll());
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        camionService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}