package com.bidiws.dto.residencesyndic;

public record ResidenceSyndicResponseDto(
        Long residenceId,
        String residenceNom,
        Long syndicId,
        String syndicNom,
        String syndicPrenom
) {}
