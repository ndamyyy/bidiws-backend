package com.bidiws.dto.residencehabitant;

public record ResidenceHabitantResponseDto(
        Long residenceId,
        String residenceNom,
        Long habitantId,
        String habitantNom,
        String habitantPrenom
) {}
