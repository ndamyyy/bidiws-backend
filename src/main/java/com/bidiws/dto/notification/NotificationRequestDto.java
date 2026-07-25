package com.bidiws.dto.notification;

import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.TypeNotification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequestDto(

        @NotNull
        Long destinataireId,

        Long arretId,

        Long residenceId,

        @NotNull
        TypeNotification type,

        @NotBlank
        String titre,

        @NotBlank
        String message,

        CanalNotification canal
) {}