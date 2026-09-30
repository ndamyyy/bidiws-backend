package com.bidiws.dto.auth;

public record LoginResponseDto(
        String accessToken,
        String refreshToken
) {}
