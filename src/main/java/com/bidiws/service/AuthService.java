package com.bidiws.service;

import com.bidiws.dto.utilisateur.UtilisateurLoginRequestDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Compteur par compte (email), pas par IP — suffisant pour ce cas
    // d'usage, pas de notion d'IP cote backend aujourd'hui.
    private static final short SEUIL_ECHECS_AVANT_VERROUILLAGE = 5;
    private static final long DUREE_VERROUILLAGE_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    // PAS de @Transactional ici : enregistrerEchec() doit persister meme
    // quand la methode se termine par une exception relancee — un
    // @Transactional englobant aurait annule ce save au rollback. Chaque
    // repository.save() gere sa propre transaction (comportement par
    // defaut de Spring Data JPA), ce qui est exactement ce qu'il faut.
    public String login(UtilisateurLoginRequestDto dto) {

        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(dto.email());

        // Verifie le verrouillage AVANT toute verification du mot de passe.
        // Email inconnu : rien a verrouiller, on laisse authenticationManager
        // produire le meme 401 generique qu'avant (pas de nouveau chemin
        // d'enumeration d'email).
        utilisateurOpt.ifPresent(this::verifierEtDeverrouillerSiExpire);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.motDePasse())
            );
        } catch (AuthenticationException ex) {
            utilisateurOpt.ifPresent(this::enregistrerEchec);
            throw ex;
        }

        Utilisateur utilisateur = utilisateurOpt
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après authentification"));

        reinitialiserEchecs(utilisateur);

        // CustomUserDetails (pas un User Spring generique) : JwtService pose
        // desormais l'ID comme sujet du token, pas l'email — voir JwtService.
        return jwtService.generateToken(new CustomUserDetails(utilisateur));
    }

    // Un compte verrouille le reste meme avec le bon mot de passe, jusqu'a
    // expiration du delai — signal different d'un mauvais mot de passe
    // (423, pas 401), message generique sans preciser la duree restante
    // (evite un timer exploitable).
    private void verifierEtDeverrouillerSiExpire(Utilisateur utilisateur) {
        if (utilisateur.getVerrouilleJusqua() == null) {
            return;
        }

        if (utilisateur.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Compte temporairement verrouillé, réessayez dans quelques minutes");
        }

        // Verrouillage expire : pas besoin d'attendre un login reussi pour
        // deverrouiller.
        utilisateur.setTentativesEchouees((short) 0);
        utilisateur.setVerrouilleJusqua(null);
        utilisateurRepository.save(utilisateur);
    }

    private void enregistrerEchec(Utilisateur utilisateur) {
        short tentatives = (short) (utilisateur.getTentativesEchouees() + 1);
        utilisateur.setTentativesEchouees(tentatives);

        if (tentatives >= SEUIL_ECHECS_AVANT_VERROUILLAGE) {
            utilisateur.setVerrouilleJusqua(LocalDateTime.now().plusMinutes(DUREE_VERROUILLAGE_MINUTES));
        }

        utilisateurRepository.save(utilisateur);
    }

    private void reinitialiserEchecs(Utilisateur utilisateur) {
        if (utilisateur.getTentativesEchouees() == 0 && utilisateur.getVerrouilleJusqua() == null) {
            return;
        }
        utilisateur.setTentativesEchouees((short) 0);
        utilisateur.setVerrouilleJusqua(null);
        utilisateurRepository.save(utilisateur);
    }
}
