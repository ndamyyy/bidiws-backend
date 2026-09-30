package com.bidiws.dto.ville;

import jakarta.validation.constraints.NotBlank;

public record VilleRequestDto(

        @NotBlank
        String nom,

        @NotBlank
        String codePostal,

        @NotBlank
        String departement
) {}
