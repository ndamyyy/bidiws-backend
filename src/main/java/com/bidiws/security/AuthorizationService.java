package com.bidiws.security;

import com.bidiws.entity.Arret;
import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.Conteneur;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.repository.ResidenceGardienRepository;
import com.bidiws.repository.ResidenceHabitantRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.ResidenceSyndicRepository;
import com.bidiws.repository.SignalementRepository;
import com.bidiws.repository.TourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final TourneeRepository tourneeRepository;
    private final ArretRepository arretRepository;
    private final SignalementRepository signalementRepository;
    private final ResidenceGardienRepository residenceGardienRepository;
    private final ResidenceHabitantRepository residenceHabitantRepository;
    private final ResidenceSyndicRepository residenceSyndicRepository;
    private final ResidenceRepository residenceRepository;
    private final CalendrierCollecteRepository calendrierCollecteRepository;
    private final CamionRepository camionRepository;
    private final ConteneurRepository conteneurRepository;

    public boolean isAssignedChauffeur(Long tourneeId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || !hasAuthority(authentication, "ROLE_CHAUFFEUR")) {
            return false;
        }

        return tourneeRepository.existsByIdAndChauffeurId(tourneeId, userDetails.getId());
    }

    public boolean isAssignedChauffeurForArret(Long arretId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || !hasAuthority(authentication, "ROLE_CHAUFFEUR")) {
            return false;
        }
        return arretRepository.existsByIdAndTourneeChauffeurId(arretId, userDetails.getId());
    }

    public boolean canAccessSignalement(Long signalementId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }
        if (signalementRepository.existsByIdAndAuteurId(signalementId, userDetails.getId())) {
            return true;
        }
        // Gardien, syndic ou mairie : uniquement pour les residences avec
        // lesquelles ils ont un rattachement reel, pas n'importe laquelle.
        if (hasAuthority(authentication, "ROLE_GARDIEN")
                && signalementRepository.existsByIdAndResidence_Gardiens_Gardien_Id(signalementId, userDetails.getId())) {
            return true;
        }
        if (hasAuthority(authentication, "ROLE_SYNDIC")
                && signalementRepository.existsByIdAndResidence_Syndics_Syndic_Id(signalementId, userDetails.getId())) {
            return true;
        }
        return hasAuthority(authentication, "ROLE_MAIRIE")
                && userDetails.getVilleId() != null
                && signalementRepository.existsByIdAndResidence_Ville_Id(signalementId, userDetails.getVilleId());
    }

    // Reservee au traitement d'un signalement (changement de statut) : contrairement
    // a canAccessSignalement, pas de bypass "c'est le mien" pour l'auteur, sinon un
    // habitant pourrait cloturer lui-meme son propre signalement.
    public boolean canModerateSignalement(Long signalementId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }
        if (hasAuthority(authentication, "ROLE_GARDIEN")) {
            return signalementRepository.existsByIdAndResidence_Gardiens_Gardien_Id(signalementId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_MAIRIE")) {
            return userDetails.getVilleId() != null
                    && signalementRepository.existsByIdAndResidence_Ville_Id(signalementId, userDetails.getVilleId());
        }
        return false;
    }

    public boolean isSelf(Long userId, Authentication authentication) {
        return authentication.getPrincipal() instanceof CustomUserDetails userDetails && userDetails.getId().equals(userId);
    }

    public boolean isGardienOfResidence(Long residenceId, Authentication authentication) {
        return authentication.getPrincipal() instanceof  CustomUserDetails userDetails && hasAuthority(authentication, "ROLE_GARDIEN" ) && residenceGardienRepository.existsByResidenceIdAndGardienId(residenceId, userDetails.getId());

    }

    public boolean isHabitantOfResidence(Long residenceId, Authentication authentication) {
        return authentication.getPrincipal() instanceof CustomUserDetails userDetails
                && hasAuthority(authentication, "ROLE_HABITANT")
                && residenceHabitantRepository.existsByResidenceIdAndHabitantId(residenceId, userDetails.getId());
    }

    public boolean isSyndicOfResidence(Long residenceId, Authentication authentication) {
        return authentication.getPrincipal() instanceof CustomUserDetails userDetails
                && hasAuthority(authentication, "ROLE_SYNDIC")
                && residenceSyndicRepository.existsByResidenceIdAndSyndicId(residenceId, userDetails.getId());
    }

    // Une mairie sans ville rattachee n'a acces a aucune residence : on ne
    // "fail open" jamais sur un rattachement absent.
    public boolean isMairieOfResidence(Long residenceId, Authentication authentication) {
        return authentication.getPrincipal() instanceof CustomUserDetails userDetails
                && hasAuthority(authentication, "ROLE_MAIRIE")
                && userDetails.getVilleId() != null
                && residenceRepository.existsByIdAndVilleId(residenceId, userDetails.getVilleId());
    }

    // Utilisee pour scoper les abonnements STOMP /topic/tournees/{id}(/position) :
    // un abonne ne doit recevoir que les tournees qui touchent une residence
    // avec laquelle il a un rattachement reel (meme regle que le reste de la classe).
    public boolean canAccessTournee(Long tourneeId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }
        if (hasAuthority(authentication, "ROLE_CHAUFFEUR")) {
            return tourneeRepository.existsByIdAndChauffeurId(tourneeId, userDetails.getId());
        }

        List<Arret> arrets = arretRepository.findByTourneeId(tourneeId);
        if (arrets.isEmpty()) {
            return false;
        }

        if (hasAuthority(authentication, "ROLE_GARDIEN")) {
            return arrets.stream().anyMatch(a ->
                    residenceGardienRepository.existsByResidenceIdAndGardienId(a.getResidence().getId(), userDetails.getId()));
        }
        if (hasAuthority(authentication, "ROLE_SYNDIC")) {
            return arrets.stream().anyMatch(a ->
                    residenceSyndicRepository.existsByResidenceIdAndSyndicId(a.getResidence().getId(), userDetails.getId()));
        }
        if (hasAuthority(authentication, "ROLE_MAIRIE")) {
            Long villeId = userDetails.getVilleId();
            return villeId != null && arrets.stream().anyMatch(a ->
                    a.getResidence().getVille() != null && villeId.equals(a.getResidence().getVille().getId()));
        }
        return false;
    }

    // Un GET par id doit rester au meme niveau de rattachement que le reste de la
    // classe : chauffeur assigne a la tournee de l'arret, ou residence liee
    // (gardien/syndic/mairie), pas juste "authentifie".
    public boolean canAccessArret(Long arretId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }
        if (hasAuthority(authentication, "ROLE_CHAUFFEUR")) {
            return arretRepository.existsByIdAndTourneeChauffeurId(arretId, userDetails.getId());
        }

        Optional<Arret> arret = arretRepository.findById(arretId);
        if (arret.isEmpty()) {
            return false;
        }
        Long residenceId = arret.get().getResidence().getId();

        if (hasAuthority(authentication, "ROLE_GARDIEN")) {
            return residenceGardienRepository.existsByResidenceIdAndGardienId(residenceId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_SYNDIC")) {
            return residenceSyndicRepository.existsByResidenceIdAndSyndicId(residenceId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_MAIRIE")) {
            return userDetails.getVilleId() != null
                    && residenceRepository.existsByIdAndVilleId(residenceId, userDetails.getVilleId());
        }
        return false;
    }

    // Meme regle de rattachement que canAccessArret, appliquee au calendrier de
    // collecte plutot qu'a l'arret : le calendrier appartient directement a une residence.
    public boolean canAccessCalendrier(Long calendrierId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        Optional<CalendrierCollecte> calendrier = calendrierCollecteRepository.findById(calendrierId);
        if (calendrier.isEmpty()) {
            return false;
        }
        Long residenceId = calendrier.get().getResidence().getId();

        if (hasAuthority(authentication, "ROLE_GARDIEN")) {
            return residenceGardienRepository.existsByResidenceIdAndGardienId(residenceId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_SYNDIC")) {
            return residenceSyndicRepository.existsByResidenceIdAndSyndicId(residenceId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_MAIRIE")) {
            return userDetails.getVilleId() != null
                    && residenceRepository.existsByIdAndVilleId(residenceId, userDetails.getVilleId());
        }
        return false;
    }

    // Un camion appartient a une ville (utilisateur.ville_id pour la mairie qui
    // le gere) : meme regle fail-closed que le reste de la classe, pour eviter
    // qu'une mairie voie/gere la flotte d'une autre ville.
    public boolean canAccessCamion(Long camionId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || !hasAuthority(authentication, "ROLE_MAIRIE")) {
            return false;
        }
        return userDetails.getVilleId() != null
                && camionRepository.existsByIdAndVilleId(camionId, userDetails.getVilleId());
    }

    // Meme regle de rattachement que canAccessCalendrier : le conteneur
    // appartient directement a une residence.
    public boolean canAccessConteneur(Long conteneurId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        Optional<Conteneur> conteneur = conteneurRepository.findById(conteneurId);
        if (conteneur.isEmpty()) {
            return false;
        }
        Long residenceId = conteneur.get().getResidence().getId();

        if (hasAuthority(authentication, "ROLE_GARDIEN")) {
            return residenceGardienRepository.existsByResidenceIdAndGardienId(residenceId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_SYNDIC")) {
            return residenceSyndicRepository.existsByResidenceIdAndSyndicId(residenceId, userDetails.getId());
        }
        if (hasAuthority(authentication, "ROLE_MAIRIE")) {
            return userDetails.getVilleId() != null
                    && residenceRepository.existsByIdAndVilleId(residenceId, userDetails.getVilleId());
        }
        return false;
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        for (String authority : authorities) {
            if (hasAuthority(authentication, authority)) {
                return true;
            }
        }
        return false;
    }
}
