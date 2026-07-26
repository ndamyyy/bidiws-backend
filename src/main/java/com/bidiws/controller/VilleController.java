package com.bidiws.controller;

import com.bidiws.dto.ville.VilleRequestDto;
import com.bidiws.dto.ville.VilleResponseDto;
import com.bidiws.service.VilleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/villes")
@RequiredArgsConstructor
public class VilleController {

    private final VilleService villeService;

    @PostMapping
    public ResponseEntity<VilleResponseDto> create(@Valid @RequestBody VilleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(villeService.create(dto));
    }

    @PutMapping("{/id}")
    public ResponseEntity<VilleResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody VilleRequestDto dto
    ) {
      return ResponseEntity.ok(villeService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VilleResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(villeService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<VilleResponseDto>> getAll() {
        return ResponseEntity.ok(villeService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<List<VilleResponseDto>> delete(@PathVariable Long id) {
        villeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
