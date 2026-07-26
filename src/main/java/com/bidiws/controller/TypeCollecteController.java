package com.bidiws.controller;

import com.bidiws.dto.typecollecte.TypeCollecteRequestDto;
import com.bidiws.dto.typecollecte.TypeCollecteResponseDto;
import com.bidiws.service.TypeCollecteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/types-collecte")
@RequiredArgsConstructor
public class TypeCollecteController {

    private final TypeCollecteService  typeCollecteService;

    @PostMapping
    public ResponseEntity<TypeCollecteResponseDto> create(@Valid @RequestBody TypeCollecteRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(typeCollecteService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TypeCollecteResponseDto> update(
            @PathVariable long id,
            @Valid @RequestBody TypeCollecteRequestDto dto
    ) {
        return ResponseEntity.ok(typeCollecteService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeCollecteResponseDto> getById(@PathVariable long id) {
        return ResponseEntity.ok(typeCollecteService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<TypeCollecteResponseDto>> getAll() {
        return ResponseEntity.ok(typeCollecteService.getAll());
    }

    public ResponseEntity<Void> delete(@PathVariable long id) {
        typeCollecteService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
