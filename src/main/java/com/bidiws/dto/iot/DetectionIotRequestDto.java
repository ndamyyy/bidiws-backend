package com.bidiws.dto.iot;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DetectionIotRequestDto(

        @NotNull
        LocalDateTime horodatage,

        // Requis uniquement si l'appareil authentifie est un lecteur monte
        // sur un camion (AppareilIot.camion renseigne) : c'est ce tag qui
        // identifie quel conteneur vient d'etre scanne. Absent/ignore si
        // l'appareil est colle directement a un conteneur precis.
        String rfidTag
) {}
