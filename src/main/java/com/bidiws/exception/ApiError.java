package com.bidiws.exception;

public record ApiError(
        int status,
        String message,
        String timestamp,
        String path
) {}
