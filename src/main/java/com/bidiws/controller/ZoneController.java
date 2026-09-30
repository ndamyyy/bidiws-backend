package com.bidiws.controller;

import com.bidiws.dto.zone.ZoneRequestDto;
import com.bidiws.dto.zone.ZoneResponseDto;
import com.bidiws.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponseDto> create(@Valid @RequestBody ZoneRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ZoneRequestDto dto
    ) {
       return ResponseEntity.ok(zoneService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoneResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(zoneService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ZoneResponseDto>> getAll(
        @RequestParam(required = false) Long villeId
    ) {
        if (villeId != null) {
            return ResponseEntity.ok(zoneService.getByVille(villeId));
        }
        return ResponseEntity.ok(zoneService.getAll());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
