package com.bidiws.dto.utilisateur;

import com.bidiws.enums.Role;

public record UtilisateurResponseDto(
        Long id,
        String email,
        String nom,
        String prenom,
        String telephone,
        Role role,
        Boolean actif
){}
