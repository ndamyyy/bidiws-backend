package com.bidiws.dto.notification;

import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.TypeNotification;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        Long destinataireId,
        Long arretId,
        Long residenceId,
        TypeNotification type,
        String titre,
        String message,
        CanalNotification canal,
        boolean lu,
        boolean envoye,
        LocalDateTime heureEnvoi,
        LocalDateTime heureLecture
) {}