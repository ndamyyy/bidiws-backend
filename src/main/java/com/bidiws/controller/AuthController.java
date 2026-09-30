package com.bidiws.controller;

import com.bidiws.dto.auth.LoginResponseDto;
import com.bidiws.dto.auth.RefreshRequestDto;
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
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody UtilisateurLoginRequestDto loginDto) {
        return ResponseEntity.ok(authService.login(loginDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@Valid @RequestBody RefreshRequestDto dto) {
        return ResponseEntity.ok(authService.refresh(dto.refreshToken()));
    }
}