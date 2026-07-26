package com.bidiws.service;

import com.bidiws.dto.utilisateur.UtilisateurLoginRequestDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    public String login(UtilisateurLoginRequestDto dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.motDePasse())
        );

        Utilisateur utilisateur = utilisateurRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après authentification"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .authorities("ROLE_" + utilisateur.getRole().name())
                .build();

        return jwtService.generateToken(userDetails);
    }
}
