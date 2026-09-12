package com.bidiws.dto.appareiliot;

import com.bidiws.enums.TypeAppareilIot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppareilIotRequestDto(

        @NotBlank
        String identifiantMateriel,

        @NotNull
        TypeAppareilIot typeAppareil,

        // Au plus un des deux : rempli si l'appareil est colle a un bac
        // precis (conteneurId) ou monte sur un camion (camionId). Aucun des
        // deux n'est requis (device enregistre mais pas encore installe).
        Long conteneurId,

        Long camionId
) {}
