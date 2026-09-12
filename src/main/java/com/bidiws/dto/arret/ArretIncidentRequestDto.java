package com.bidiws.dto.arret;

import jakarta.validation.constraints.NotBlank;

public record ArretIncidentRequestDto(

        @NotBlank
        String descriptionIncident,

        String photoIncidentUrl
) {}