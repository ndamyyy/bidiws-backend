package com.bidiws.controller;

import com.bidiws.dto.pushtoken.PushTokenRequestDto;
import com.bidiws.dto.pushtoken.PushTokenResponseDto;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.service.PushTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @PostMapping
    public ResponseEntity<PushTokenResponseDto> enregistrer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PushTokenRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pushTokenService.enregistrer(userDetails.getId(), dto));
    }

    @PatchMapping("/desactiver")
    public ResponseEntity<Void> desactiver(@RequestParam String token) {
        pushTokenService.desactiver(token);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String token) {
        pushTokenService.delete(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PushTokenResponseDto>> getMesTokens(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pushTokenService.getByUtilisateur(userDetails.getId()));
    }
}