package com.bidiws.dto.pushtoken;

import com.bidiws.enums.Plateforme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushTokenRequestDto(

        @NotBlank
        String token,

        @NotNull
        Plateforme plateforme
) {}