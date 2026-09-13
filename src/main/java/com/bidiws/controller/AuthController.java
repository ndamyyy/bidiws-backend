package com.bidiws.controller;

import com.bidiws.dto.utilisateur.InscriptionResponseDto;
import com.bidiws.dto.utilisateur.UtilisateurLoginRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurRegisterRequestDto;
import com.bidiws.service.AuthService;
import com.bidiws.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UtilisateurService utilisateurService;

    @PostMapping("/register")
    public ResponseEntity<InscriptionResponseDto> register(@Valid @RequestBody UtilisateurRegisterRequestDto registerDto) {
        InscriptionResponseDto responseDto = utilisateurService.register(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UtilisateurLoginRequestDto loginDto) {
        String token = authService.login(loginDto);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
