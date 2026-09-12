package com.bidiws.controller;

import com.bidiws.dto.appareiliot.AppareilIotCreeResponseDto;
import com.bidiws.dto.appareiliot.AppareilIotImportResultatDto;
import com.bidiws.dto.appareiliot.AppareilIotRequestDto;
import com.bidiws.dto.appareiliot.AppareilIotResponseDto;
import com.bidiws.service.AppareilIotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Gestion des appareils IoT reservee a ADMIN : ce sont des identifiants
// materiels credentialises (cle API), pas une ressource metier classique
// qu'une MAIRIE gererait au quotidien.
@RestController
@RequestMapping("/appareils-iot")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AppareilIotController {

    private final AppareilIotService appareilIotService;

    @PostMapping
    public ResponseEntity<AppareilIotCreeResponseDto> create(@Valid @RequestBody AppareilIotRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appareilIotService.create(dto));
    }

    // Import en masse : une ligne par appareil, memes champs que la
    // creation unitaire. Lecture seule sur les echecs — chaque ligne en
    // erreur est rapportee avec sa raison, n'empeche pas les autres.
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppareilIotImportResultatDto> importerCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(appareilIotService.importerCsv(file));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppareilIotResponseDto> update(@PathVariable Long id, @Valid @RequestBody AppareilIotRequestDto dto) {
        return ResponseEntity.ok(appareilIotService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppareilIotResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appareilIotService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppareilIotResponseDto>> getAll() {
        return ResponseEntity.ok(appareilIotService.getAll());
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        appareilIotService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerer-cle")
    public ResponseEntity<AppareilIotCreeResponseDto> regenererCle(@PathVariable Long id) {
        return ResponseEntity.ok(appareilIotService.regenererCle(id));
    }
}
