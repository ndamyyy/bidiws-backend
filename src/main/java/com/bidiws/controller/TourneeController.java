package com.bidiws.controller;

import com.bidiws.dto.tournee.TourneeRequestDto;
import com.bidiws.dto.tournee.TourneeResponseDto;
import com.bidiws.service.TourneeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tournees")
@RequiredArgsConstructor
public class TourneeController {

    private final TourneeService tourneeService;

    @PostMapping
    public ResponseEntity<TourneeResponseDto> create(@Valid @RequestBody TourneeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourneeService.create(dto));
    }

    @PatchMapping("/{id}/demarrer")
    public ResponseEntity<TourneeResponseDto> demarrer(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.demarrer(id));
    }

    @PatchMapping("/{id}/terminer")
    public ResponseEntity<TourneeResponseDto> terminer(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.terminer(id));
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<TourneeResponseDto> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.annuler(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TourneeResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tourneeService.getById(id));
    }

    @GetMapping
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