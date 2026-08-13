package com.bidiws.dto.appareiliot;

import com.bidiws.enums.TypeAppareilIot;

import java.time.LocalDateTime;

public record AppareilIotResponseDto(
        Long id,
        String identifiantMateriel,
        TypeAppareilIot typeAppareil,
        Long conteneurId,
        String conteneurCode,
        Long camionId,
        String camionImmatriculation,
        Boolean actif,
        LocalDateTime createdAt
) {}
