package com.bidiws.dto.pushtoken;

import com.bidiws.enums.Plateforme;

import java.time.LocalDateTime;

public record PushTokenResponseDto(
        Long id,
        Long utilisateurId,
        String token,
        Plateforme plateforme,
        boolean actif,
        LocalDateTime createdAt
) {}