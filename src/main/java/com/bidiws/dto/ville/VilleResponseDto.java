package com.bidiws.dto.ville;

public record VilleResponseDto(
        Long id,
        String nom,
        String codePostal,
        String departement
) {}
