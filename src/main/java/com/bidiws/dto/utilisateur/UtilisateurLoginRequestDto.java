package com.bidiws.dto.utilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UtilisateurLoginRequestDto(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String motDePasse
) {}
