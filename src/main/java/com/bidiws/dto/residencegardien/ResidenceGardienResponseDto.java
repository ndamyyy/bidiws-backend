package com.bidiws.dto.residencegardien;

public record ResidenceGardienResponseDto(
        Long residenceId,
        String residenceNom,
        Long gardienId,
        String gardienNom,
        String gardienPrenom,
        boolean principal
) {}
