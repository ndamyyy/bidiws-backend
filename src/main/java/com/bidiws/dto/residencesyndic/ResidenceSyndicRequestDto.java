package com.bidiws.dto.residencesyndic;

import jakarta.validation.constraints.NotNull;

public record ResidenceSyndicRequestDto(

        @NotNull
        Long residenceId,

        @NotNull
        Long syndicId
) {}
