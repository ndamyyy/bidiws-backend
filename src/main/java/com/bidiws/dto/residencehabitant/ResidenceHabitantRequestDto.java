package com.bidiws.dto.residencehabitant;

import jakarta.validation.constraints.NotNull;

public record ResidenceHabitantRequestDto(

        @NotNull
        Long residenceId,

        @NotNull
        Long habitantId
) {}
