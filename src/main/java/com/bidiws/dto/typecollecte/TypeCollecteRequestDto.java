package com.bidiws.dto.typecollecte;

import jakarta.validation.constraints.NotBlank;

    public record TypeCollecteRequestDto(

            @NotBlank
            String code,

            @NotBlank
            String libelle,

            String couleur,

            String icone
) {}
