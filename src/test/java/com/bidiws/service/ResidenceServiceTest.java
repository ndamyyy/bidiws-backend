package com.bidiws.service;

import com.bidiws.dto.residence.ResidenceRequestDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Utilisateur;
import com.bidiws.entity.Ville;
import com.bidiws.enums.Role;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ResidenceGardienRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.ResidenceSyndicRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.repository.ZoneRepository;
import com.bidiws.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie l'isolation des donnees entre syndics et entre villes, demandee
 * par le client : un syndic ne doit voir/modifier que ses propres
 * residences, une mairie que celles de sa ville, un admin garde tout.
 */
@ExtendWith(MockitoExtension.class)
class ResidenceServiceTest {

    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private VilleRepository villeRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private ResidenceGardienRepository residenceGardienRepository;
    @Mock
    private ResidenceSyndicRepository residenceSyndicRepository;
    @Mock
    private ArretRepository arretRepository;

    @InjectMocks
    private ResidenceService residenceService;

    private static final Long VILLE_A_ID = 10L;
    private static final Long VILLE_B_ID = 20L;
    private static final Long RESIDENCE_ID = 1L;

    private CustomUserDetails utilisateur(Long id, Role role, Long villeId) {
        Utilisateur.UtilisateurBuilder builder = Utilisateur.builder()
                .id(id)
                .email("user" + id + "@bidiws.com")
                .motDePasse("hash")
                .role(role)
                .actif(true);
        if (villeId != null) {
            builder.ville(Ville.builder().id(villeId).build());
        }
        return new CustomUserDetails(builder.build());
    }

    private Residence residenceDansVille(Long villeId) {
        return Residence.builder()
                .id(RESIDENCE_ID)
                .nom("Résidence Test")
                .adresse("1 rue Test")
                .ville(Ville.builder().id(villeId).build())
                .build();
    }

    @Test
    void unSyndicNePeutPasVoirLaResidenceDunAutreSyndic() {
        CustomUserDetails syndicA = utilisateur(1L, Role.SYNDIC, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 1L)).thenReturn(false);

        assertThatThrownBy(() -> residenceService.getById(RESIDENCE_ID, syndicA))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non autorisé");
    }

    @Test
    void unSyndicPeutVoirSaPropreResidence() {
        CustomUserDetails syndicA = utilisateur(1L, Role.SYNDIC, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 1L)).thenReturn(true);

        assertThat(residenceService.getById(RESIDENCE_ID, syndicA).id()).isEqualTo(RESIDENCE_ID);
    }

    @Test
    void uneMairieNeVoitPasUneResidenceDuneAutreVille() {
        CustomUserDetails mairieVilleA = utilisateur(2L, Role.MAIRIE, VILLE_A_ID);
        Residence residenceVilleB = residenceDansVille(VILLE_B_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residenceVilleB));

        assertThatThrownBy(() -> residenceService.getById(RESIDENCE_ID, mairieVilleA))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non autorisé");
    }

    @Test
    void uneMairieVoitLesResidencesDeSaVille() {
        CustomUserDetails mairieVilleA = utilisateur(2L, Role.MAIRIE, VILLE_A_ID);
        Residence residenceVilleA = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residenceVilleA));

        assertThat(residenceService.getById(RESIDENCE_ID, mairieVilleA).id()).isEqualTo(RESIDENCE_ID);
    }

    @Test
    void uneMairieSansVilleRattacheeNaAccesANulleResidence() {
        CustomUserDetails mairieSansVille = utilisateur(2L, Role.MAIRIE, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));

        assertThatThrownBy(() -> residenceService.getById(RESIDENCE_ID, mairieSansVille))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non autorisé");
    }

    @Test
    void adminAccedeATouteResidenceQuelleQueSoitLaVilleOuLeSyndic() {
        CustomUserDetails admin = utilisateur(3L, Role.ADMIN, null);
        Residence residence = residenceDansVille(VILLE_B_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));

        assertThat(residenceService.getById(RESIDENCE_ID, admin).id()).isEqualTo(RESIDENCE_ID);
    }

    @Test
    void unHabitantNaAccesAAucuneResidence() {
        CustomUserDetails habitant = utilisateur(4L, Role.HABITANT, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));

        assertThatThrownBy(() -> residenceService.getById(RESIDENCE_ID, habitant))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non autorisé");
    }

    private ResidenceRequestDto dtoPour(Long villeId) {
        return new ResidenceRequestDto(
                "Résidence Test", "1 rue Test", null, "75000",
                villeId, null, null, null, null, null
        );
    }

    @Test
    void unSyndicNePeutPasModifierLaResidenceDunAutreSyndic() {
        CustomUserDetails syndicB = utilisateur(5L, Role.SYNDIC, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 5L)).thenReturn(false);

        assertThatThrownBy(() -> residenceService.update(RESIDENCE_ID, dtoPour(VILLE_A_ID), syndicB))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("gérez pas");
    }

    @Test
    void uneMairieNePeutPasModifierUneResidenceDuneAutreVille() {
        CustomUserDetails mairieVilleA = utilisateur(2L, Role.MAIRIE, VILLE_A_ID);
        Residence residenceVilleB = residenceDansVille(VILLE_B_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residenceVilleB));

        assertThatThrownBy(() -> residenceService.update(RESIDENCE_ID, dtoPour(VILLE_B_ID), mairieVilleA))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("votre ville");
    }

    @Test
    void unSyndicNePeutPasChangerLaVilleDUneResidence() {
        CustomUserDetails syndic = utilisateur(5L, Role.SYNDIC, null);
        Residence residenceVilleA = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residenceVilleA));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 5L)).thenReturn(true);

        assertThatThrownBy(() -> residenceService.update(RESIDENCE_ID, dtoPour(VILLE_B_ID), syndic))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Un syndic ne peut pas changer la ville");
    }

    @Test
    void unSyndicPeutModifierSaResidenceSansChangerLaVille() {
        CustomUserDetails syndic = utilisateur(5L, Role.SYNDIC, null);
        Residence residenceVilleA = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residenceVilleA));
        when(residenceSyndicRepository.existsByResidenceIdAndSyndicId(RESIDENCE_ID, 5L)).thenReturn(true);
        when(villeRepository.findById(VILLE_A_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_A_ID).build()));
        when(residenceRepository.save(org.mockito.ArgumentMatchers.any(Residence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> residenceService.update(RESIDENCE_ID, dtoPour(VILLE_A_ID), syndic))
                .doesNotThrowAnyException();
    }

    @Test
    void desactiverEchoueSiUnArretDeCetteResidenceEstSurUneTourneeActive() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));
        when(arretRepository.existsActifByResidenceId(RESIDENCE_ID)).thenReturn(true);

        assertThatThrownBy(() -> residenceService.desactiver(RESIDENCE_ID, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tournée active");
    }

    @Test
    void desactiverFonctionneSansArretActif() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        Residence residence = residenceDansVille(VILLE_A_ID);

        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence));
        when(arretRepository.existsActifByResidenceId(RESIDENCE_ID)).thenReturn(false);

        assertThatCode(() -> residenceService.desactiver(RESIDENCE_ID, admin)).doesNotThrowAnyException();

        verify(residenceRepository).save(residence);
    }
}
