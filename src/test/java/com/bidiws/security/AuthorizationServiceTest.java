package com.bidiws.security;

import com.bidiws.entity.Arret;
import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.Conteneur;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Tournee;
import com.bidiws.entity.Utilisateur;
import com.bidiws.entity.Ville;
import com.bidiws.entity.Zone;
import com.bidiws.enums.Role;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifie le scoping des abonnements STOMP par tournee (canAccessTournee) :
 * un abonne a /topic/tournees/{id}(/position) ne doit y acceder que s'il a
 * un rattachement reel a une residence touchee par cette tournee, meme
 * regle d'isolation que le reste de la classe (cf. isSyndicOfResidence,
 * isMairieOfResidence).
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private ArretRepository arretRepository;
    @Mock
    private SignalementRepository signalementRepository;
    @Mock
    private ResidenceGardienRepository residenceGardienRepository;
    @Mock
    private ResidenceHabitantRepository residenceHabitantRepository;
    @Mock
    private ResidenceSyndicRepository residenceSyndicRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private CalendrierCollecteRepository calendrierCollecteRepository;
    @Mock
    private CamionRepository camionRepository;
    @Mock
    private ConteneurRepository conteneurRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private static final Long TOURNEE_ID = 100L;
    private static final Long ARRET_ID = 200L;
    private static final Long CALENDRIER_ID = 300L;
    private static final Long SIGNALEMENT_ID = 400L;
    private static final Long CAMION_ID = 500L;
    private static final Long CONTENEUR_ID = 600L;
    private static final Long RESIDENCE_ID = 1L;
    private static final Long VILLE_A_ID = 10L;
    private static final Long VILLE_B_ID = 20L;

    private Authentication authentification(Long id, Role role, Long villeId) {
        Utilisateur.UtilisateurBuilder builder = Utilisateur.builder()
                .id(id)
                .email("user" + id + "@bidiws.com")
                .motDePasse("hash")
                .role(role)
                .actif(true);
        if (villeId != null) {
            builder.ville(Ville.builder().id(villeId).build());
        }
        CustomUserDetails userDetails = new CustomUserDetails(builder.build());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Arret arretPourResidenceDansVille(Long villeId) {
        Residence residence = Residence.builder()
                .id(RESIDENCE_ID)
                .ville(Ville.builder().id(villeId).build())
                .build();
        return Arret.builder().residence(residence).build();
    }

    private Tournee tourneeAvecZoneDansVille(Long villeId) {
        Zone zone = Zone.builder().ville(Ville.builder().id(villeId).build()).build();
        return Tournee.builder().id(TOURNEE_ID).zone(zone).build();
    }

    private CalendrierCollecte calendrierPourResidence() {
        Residence residence = Residence.builder().id(RESIDENCE_ID).build();
        return CalendrierCollecte.builder().id(CALENDRIER_ID).residence(residence).build();
    }

    @Test
    void adminAccedeAToutesLesTournees() {
        Authentication admin = authentification(1L, Role.ADMIN, null);

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, admin)).isTrue();
    }

    @Test
    void leChauffeurAssigneAccedeALaTournee() {
        Authentication chauffeur = authentification(2L, Role.CHAUFFEUR, null);
        when(tourneeRepository.existsByIdAndChauffeurId(TOURNEE_ID, 2L)).thenReturn(true);

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, chauffeur)).isTrue();
    }

    @Test
    void unChauffeurNonAssigneNaPasAcces() {
        Authentication chauffeur = authentification(2L, Role.CHAUFFEUR, null);
        when(tourneeRepository.existsByIdAndChauffeurId(TOURNEE_ID, 2L)).thenReturn(false);

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, chauffeur)).isFalse();
    }

    @Test
    void unSyndicDeLaResidenceConcerneeAAcces() {
        Authentication syndic = authentification(3L, Role.SYNDIC, null);
        when(arretRepository.findByTourneeId(TOURNEE_ID)).thenReturn(List.of(arretPourResidenceDansVille(VILLE_A_ID)));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 3L)).thenReturn(true);

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, syndic)).isTrue();
    }

    @Test
    void unSyndicDuneAutreResidenceNaPasAcces() {
        Authentication syndic = authentification(3L, Role.SYNDIC, null);
        when(arretRepository.findByTourneeId(TOURNEE_ID)).thenReturn(List.of(arretPourResidenceDansVille(VILLE_A_ID)));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 3L)).thenReturn(false);

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, syndic)).isFalse();
    }

    @Test
    void uneMairieDeLaVilleConcerneeAAcces() {
        Authentication mairie = authentification(4L, Role.MAIRIE, VILLE_A_ID);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tourneeAvecZoneDansVille(VILLE_A_ID)));

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, mairie)).isTrue();
    }

    @Test
    void uneMairieDuneAutreVilleNaPasAcces() {
        Authentication mairie = authentification(4L, Role.MAIRIE, VILLE_B_ID);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tourneeAvecZoneDansVille(VILLE_A_ID)));

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, mairie)).isFalse();
    }

    @Test
    void uneMairieSansVilleRattacheeNaAccesAAucuneTournee() {
        Authentication mairieSansVille = authentification(4L, Role.MAIRIE, null);

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, mairieSansVille)).isFalse();
    }

    // Bug confirme en conditions reelles (frontend BIDIWS) : une tournee
    // flambant neuve (0 arret, ex. juste apres creation par un admin) 403ait
    // pour la mairie proprietaire sur GET /arrets/tournee/{id} alors qu'elle
    // apparaissait bien dans son propre GET /tournees — canAccessTournee
    // faisait passer TOUS les roles non-admin/chauffeur par le rattachement
    // via arrets, y compris MAIRIE qui a pourtant un lien direct zone/ville
    // sur la tournee elle-meme, independant des arrets deja poses.
    @Test
    void uneMairieDeLaVilleConcerneeAAccesMemeSansArret() {
        Authentication mairie = authentification(4L, Role.MAIRIE, VILLE_A_ID);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tourneeAvecZoneDansVille(VILLE_A_ID)));

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, mairie)).isTrue();
    }

    @Test
    void aucunArretPourLaTourneeRefuseLAccesAuxRolesNonAdminNonMairie() {
        Authentication syndic = authentification(3L, Role.SYNDIC, null);
        when(arretRepository.findByTourneeId(TOURNEE_ID)).thenReturn(List.of());

        assertThat(authorizationService.canAccessTournee(TOURNEE_ID, syndic)).isFalse();
    }

    // ── canAccessArret ─────────────────────────────────────────────

    @Test
    void unGardienDeLaResidenceDeLArretAAcces() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arretPourResidenceDansVille(VILLE_A_ID)));
        when(residenceGardienRepository.existsByResidenceIdAndGardienId(RESIDENCE_ID, 5L)).thenReturn(true);

        assertThat(authorizationService.canAccessArret(ARRET_ID, gardien)).isTrue();
    }

    @Test
    void unGardienDuneAutreResidenceNaPasAccesALArret() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arretPourResidenceDansVille(VILLE_A_ID)));
        when(residenceGardienRepository.existsByResidenceIdAndGardienId(RESIDENCE_ID, 5L)).thenReturn(false);

        assertThat(authorizationService.canAccessArret(ARRET_ID, gardien)).isFalse();
    }

    @Test
    void unChauffeurAssigneALArretAAcces() {
        Authentication chauffeur = authentification(6L, Role.CHAUFFEUR, null);
        when(arretRepository.existsByIdAndTourneeChauffeurId(ARRET_ID, 6L)).thenReturn(true);

        assertThat(authorizationService.canAccessArret(ARRET_ID, chauffeur)).isTrue();
    }

    @Test
    void unHabitantNaJamaisAccesADirectementAUnArret() {
        Authentication habitant = authentification(7L, Role.HABITANT, null);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arretPourResidenceDansVille(VILLE_A_ID)));

        assertThat(authorizationService.canAccessArret(ARRET_ID, habitant)).isFalse();
    }

    // ── canAccessCalendrier ────────────────────────────────────────

    @Test
    void unSyndicDeLaResidenceDuCalendrierAAcces() {
        Authentication syndic = authentification(3L, Role.SYNDIC, null);
        when(calendrierCollecteRepository.findById(CALENDRIER_ID)).thenReturn(Optional.of(calendrierPourResidence()));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 3L)).thenReturn(true);

        assertThat(authorizationService.canAccessCalendrier(CALENDRIER_ID, syndic)).isTrue();
    }

    @Test
    void unSyndicDuneAutreResidenceNaPasAccesAuCalendrier() {
        Authentication syndic = authentification(3L, Role.SYNDIC, null);
        when(calendrierCollecteRepository.findById(CALENDRIER_ID)).thenReturn(Optional.of(calendrierPourResidence()));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 3L)).thenReturn(false);

        assertThat(authorizationService.canAccessCalendrier(CALENDRIER_ID, syndic)).isFalse();
    }

    // ── canModerateSignalement ─────────────────────────────────────

    @Test
    void unGardienDeLaResidenceDuSignalementPeutLeModerer() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(signalementRepository.existsByIdAndResidence_Gardiens_Gardien_Id(SIGNALEMENT_ID, 5L)).thenReturn(true);

        assertThat(authorizationService.canModerateSignalement(SIGNALEMENT_ID, gardien)).isTrue();
    }

    @Test
    void unGardienDuneAutreResidenceNePeutPasModererLeSignalement() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(signalementRepository.existsByIdAndResidence_Gardiens_Gardien_Id(SIGNALEMENT_ID, 5L)).thenReturn(false);

        assertThat(authorizationService.canModerateSignalement(SIGNALEMENT_ID, gardien)).isFalse();
    }

    @Test
    void lAuteurDuSignalementNePeutPasLeModererSansRattachement() {
        // Contrairement a canAccessSignalement, pas de bypass "c'est le mien" :
        // un habitant qui a cree le signalement ne peut pas en changer le statut.
        Authentication habitant = authentification(8L, Role.HABITANT, null);

        assertThat(authorizationService.canModerateSignalement(SIGNALEMENT_ID, habitant)).isFalse();
    }

    // ── canAccessCamion ────────────────────────────────────────────

    @Test
    void adminAccedeAToutCamion() {
        Authentication admin = authentification(1L, Role.ADMIN, null);

        assertThat(authorizationService.canAccessCamion(CAMION_ID, admin)).isTrue();
    }

    @Test
    void uneMairieDeLaVilleDuCamionAAcces() {
        Authentication mairie = authentification(4L, Role.MAIRIE, VILLE_A_ID);
        when(camionRepository.existsByIdAndVilleId(CAMION_ID, VILLE_A_ID)).thenReturn(true);

        assertThat(authorizationService.canAccessCamion(CAMION_ID, mairie)).isTrue();
    }

    @Test
    void uneMairieDuneAutreVilleNaPasAccesAuCamion() {
        Authentication mairie = authentification(4L, Role.MAIRIE, VILLE_B_ID);
        when(camionRepository.existsByIdAndVilleId(CAMION_ID, VILLE_B_ID)).thenReturn(false);

        assertThat(authorizationService.canAccessCamion(CAMION_ID, mairie)).isFalse();
    }

    @Test
    void uneMairieSansVilleRattacheeNaAccesAAucunCamion() {
        Authentication mairieSansVille = authentification(4L, Role.MAIRIE, null);

        assertThat(authorizationService.canAccessCamion(CAMION_ID, mairieSansVille)).isFalse();
    }

    @Test
    void unGardienNaJamaisAccesAUnCamion() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);

        assertThat(authorizationService.canAccessCamion(CAMION_ID, gardien)).isFalse();
    }

    // ── canAccessConteneur ─────────────────────────────────────────

    private Conteneur conteneurPourResidenceDansVille(Long villeId) {
        Residence residence = Residence.builder()
                .id(RESIDENCE_ID)
                .ville(Ville.builder().id(villeId).build())
                .build();
        return Conteneur.builder().id(CONTENEUR_ID).residence(residence).build();
    }

    @Test
    void unGardienDeLaResidenceDuConteneurAAcces() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(conteneurPourResidenceDansVille(VILLE_A_ID)));
        when(residenceGardienRepository.existsByResidenceIdAndGardienId(RESIDENCE_ID, 5L)).thenReturn(true);

        assertThat(authorizationService.canAccessConteneur(CONTENEUR_ID, gardien)).isTrue();
    }

    @Test
    void unGardienDuneAutreResidenceNaPasAccesAuConteneur() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(conteneurPourResidenceDansVille(VILLE_A_ID)));
        when(residenceGardienRepository.existsByResidenceIdAndGardienId(RESIDENCE_ID, 5L)).thenReturn(false);

        assertThat(authorizationService.canAccessConteneur(CONTENEUR_ID, gardien)).isFalse();
    }

    @Test
    void uneMairieDeLaVilleDuConteneurAAcces() {
        Authentication mairie = authentification(4L, Role.MAIRIE, VILLE_A_ID);
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(conteneurPourResidenceDansVille(VILLE_A_ID)));
        when(residenceRepository.existsByIdAndVilleId(RESIDENCE_ID, VILLE_A_ID)).thenReturn(true);

        assertThat(authorizationService.canAccessConteneur(CONTENEUR_ID, mairie)).isTrue();
    }

    @Test
    void conteneurIntrouvableRefuseLAccesAuxRolesNonAdmin() {
        Authentication gardien = authentification(5L, Role.GARDIEN, null);
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.empty());

        assertThat(authorizationService.canAccessConteneur(CONTENEUR_ID, gardien)).isFalse();
    }

    @Test
    void adminAccedeAToutConteneur() {
        Authentication admin = authentification(1L, Role.ADMIN, null);

        assertThat(authorizationService.canAccessConteneur(CONTENEUR_ID, admin)).isTrue();
    }

    // ── isHabitantOfResidence ──────────────────────────────────────
    // Manquait entierement (seul isGardienOfResidence existait) : un
    // HABITANT consultant le calendrier de sa propre residence via GET
    // /calendriers-collecte/residence/{id} recevait 403 alors que c'est
    // precisement ce role qui doit voir cette page (HabitantHomePage).

    @Test
    void unHabitantDeCetteResidenceAAcces() {
        Authentication habitant = authentification(7L, Role.HABITANT, null);
        when(residenceHabitantRepository.existsByResidenceIdAndHabitantId(RESIDENCE_ID, 7L)).thenReturn(true);

        assertThat(authorizationService.isHabitantOfResidence(RESIDENCE_ID, habitant)).isTrue();
    }

    @Test
    void unHabitantDuneAutreResidenceNaPasAcces() {
        Authentication habitant = authentification(7L, Role.HABITANT, null);
        when(residenceHabitantRepository.existsByResidenceIdAndHabitantId(RESIDENCE_ID, 7L)).thenReturn(false);

        assertThat(authorizationService.isHabitantOfResidence(RESIDENCE_ID, habitant)).isFalse();
    }
}
