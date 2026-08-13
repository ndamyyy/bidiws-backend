package com.bidiws.controller;

import com.bidiws.dto.iot.DetectionIotRequestDto;
import com.bidiws.dto.iot.DetectionIotResponseDto;
import com.bidiws.entity.AppareilIot;
import com.bidiws.service.IotDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Chaine de securite separee (SecurityConfig.iotFilterChain) : pas de JWT
// ici, l'appareil est authentifie par cle API (X-Device-Api-Key) via
// DeviceApiKeyAuthenticationFilter, qui pose directement l'AppareilIot
// comme principal.
@RestController
@RequestMapping("/iot")
@RequiredArgsConstructor
public class IotController {

    private final IotDetectionService iotDetectionService;

    @PostMapping("/detections")
    public ResponseEntity<DetectionIotResponseDto> enregistrerDetection(
            @AuthenticationPrincipal AppareilIot appareil,
            @Valid @RequestBody DetectionIotRequestDto dto
    ) {
        return ResponseEntity.ok(iotDetectionService.enregistrerDetection(appareil, dto));
    }
}
