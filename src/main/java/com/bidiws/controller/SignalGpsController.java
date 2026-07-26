package com.bidiws.controller;

import com.bidiws.dto.signalgps.SignalGpsRequestDto;
import com.bidiws.dto.signalgps.SignalGpsResponseDto;
import com.bidiws.service.SignalGpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/signaux-gps")
@RequiredArgsConstructor
public class SignalGpsController {

    private final SignalGpsService signalGpsService;

    @PostMapping
    public ResponseEntity<SignalGpsResponseDto> enregistrer(@Valid @RequestBody SignalGpsRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signalGpsService.enregistrer(dto));
    }

    @GetMapping("/tournee/{tourneeId}/dernier")
    public ResponseEntity<SignalGpsResponseDto> getDernierSignal(@PathVariable Long tourneeId) {
        return ResponseEntity.ok(signalGpsService.getDernierSignal(tourneeId));
    }

    @GetMapping("/tournee/{tourneeId}/trajet")
    public ResponseEntity<List<SignalGpsResponseDto>> getTrajet(@PathVariable Long tourneeId) {
        return ResponseEntity.ok(signalGpsService.getTrajet(tourneeId));
    }

    @GetMapping("/tournee/{tourneeId}/periode")
    public ResponseEntity<List<SignalGpsResponseDto>> getEntreDates(
            @PathVariable Long tourneeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin
    ) {
        return ResponseEntity.ok(signalGpsService.getEntreDates(tourneeId, debut, fin));
    }
}