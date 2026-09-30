package com.bidiws.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(

        @NotBlank
        String refreshToken

) {}
