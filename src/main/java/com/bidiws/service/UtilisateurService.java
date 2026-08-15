package com.bidiws.service;

import com.bidiws.dto.utilisateur.*;
import com.bidiws.entity.Utilisateur;
import com.bidiws.entity.Ville;
import com.bidiws.enums.Role;
import com.bidiws.enums.StatutTournee;
import com.bidiws.repository.ChauffeurCamionRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.repository.VilleRepository;
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
    private final VilleRepository villeRepository;
    private final ChauffeurCamionRepository chauffeurCamionRepository;
    private final TourneeRepository tourneeRepository;
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

        if (!actif && utilisateur.getRole() == Role.CHAUFFEUR) {
            verifierPasDaffectationActiveChauffeur(id);
        }

        utilisateur.setActif(actif);
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public UtilisateurResponseDto changerRole(Long id, Role role) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        // Promouvoir vers CHAUFFEUR n'a pas cette notion d'affectation active a
        // verifier ; seul le fait de RETIRER le role a quelqu'un qui l'a deja
        // est concerne, meme logique que setActif(false).
        if (utilisateur.getRole() == Role.CHAUFFEUR && role != Role.CHAUFFEUR) {
            verifierPasDaffectationActiveChauffeur(id);
        }

        utilisateur.setRole(role);
        return toResponseDto(utilisateurRepository.save(utilisateur));
    }

    // Utilise par setActif(false) et changerRole (retrait du role CHAUFFEUR) :
    // un chauffeur avec une affectation camion active (dateFin == null) ou une
    // tournee EN_COURS ne peut etre ni desactive ni change de role, meme
    // raisonnement que CamionService.desactiver cote camion.
    private void verifierPasDaffectationActiveChauffeur(Long chauffeurId) {
        boolean affectationActive = chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(chauffeurId).isPresent();
        boolean tourneeEnCours = tourneeRepository.existsByChauffeurIdAndStatut(chauffeurId, StatutTournee.EN_COURS);

        if (affectationActive || tourneeEnCours) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de désactiver : ce chauffeur a une affectation active ou une tournée en cours");
        }
    }

    // Rattache un compte MAIRIE a la ville dont il pourra voir/gerer les
    // residences. villeId == null retire le rattachement.
    @Transactional
    public UtilisateurResponseDto changerVille(Long id, Long villeId) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (utilisateur.getRole() != Role.MAIRIE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le rattachement à une ville n'est possible que pour un compte MAIRIE");
        }

        if (villeId == null) {
            utilisateur.setVille(null);
        } else {
            Ville ville = villeRepository.findById(villeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));
            utilisateur.setVille(ville);
        }

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
                u.getActif(),
                u.getVille() != null ? u.getVille().getId() : null
        );
    }
}
