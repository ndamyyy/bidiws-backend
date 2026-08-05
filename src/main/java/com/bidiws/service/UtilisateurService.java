package com.bidiws.service;

import com.bidiws.dto.utilisateur.*;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
                .role(Role.HABITANT)
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

    public UtilisateurResponseDto createByAdmin(UtilisateurAdminCreateRequestDto dto) {

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

    @Transactional(readOnly = true)
    public List<UtilisateurResponseDto> rechercher(String nom, String prenom, Role role) {

        List<Utilisateur> resultats;

        if (nom != null || prenom != null) {
            resultats = utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
                    nom != null ? nom : "", prenom != null ? prenom : "");
        } else if (role != null) {
            resultats = utilisateurRepository.findByRole(role);
        } else {
            resultats = utilisateurRepository.findAll();
        }

        return resultats.stream().map(this::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponseDto getById(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return toResponseDto(utilisateur);
    }

    @Transactional
    public void setActif(Long id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        utilisateur.setActif(actif);
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public UtilisateurResponseDto changerRole(Long id, Role role) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        utilisateur.setRole(role);
        return toResponseDto(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void resetMotDePasseByAdmin(Long id, ResetPasswordRequestDto dto) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        utilisateur.setMotDePasse(passwordEncoder.encode(dto.nouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
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
                u.getId(),
                u.getEmail(),
                u.getNom(),
                u.getPrenom(),
                u.getTelephone(),
                u.getRole(),
                u.getActif()
        );
    }
}
