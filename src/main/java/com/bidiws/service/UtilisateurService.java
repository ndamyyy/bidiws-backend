package com.bidiws.service;

import com.bidiws.dto.utilisateur.ChangePasswordRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurRegisterRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurResponseDto;
import com.bidiws.dto.utilisateur.UtilisateurUpdateRequestDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UtilisateurResponseDto register(UtilisateurRegisterRequestDto dto) {

        if (utilisateurRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .email(dto.email())
                .motDePasse(passwordEncoder.encode(dto.motDePasse()))
                .nom(dto.nom())
                .prenom(dto.prenom())
                .telephone(dto.telephone())
                .role(dto.role())
                .actif(true)
                .build();

        return toResponseDto(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurResponseDto update(Long id, UtilisateurUpdateRequestDto dto) {

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (!utilisateur.getEmail().equals(dto.email())
                && utilisateurRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }

        utilisateur.setNom(dto.nom());
        utilisateur.setPrenom(dto.prenom());
        utilisateur.setEmail(dto.email());
        utilisateur.setTelephone(dto.telephone());

        return toResponseDto(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void changerMotDePasse(Long id, ChangePasswordRequestDto dto) {

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (!passwordEncoder.matches(dto.ancienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ancien mot de passe incorrect");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(dto.nouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
    }

    private UtilisateurResponseDto toResponseDto(Utilisateur u) {
        return new UtilisateurResponseDto(
                u.getId(), u.getEmail(), u.getNom(), u.getPrenom(),
                u.getTelephone(), u.getRole(), u.getActif()
        );
    }
}