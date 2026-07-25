package com.bidiws.dto.zone;

public record ZoneResponseDto(
        Long id,
        Long villeId,
        String villeNom,
        String nom,
        String code,
        String description
) {}
