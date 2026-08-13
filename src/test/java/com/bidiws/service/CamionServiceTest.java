package com.bidiws.service;

import com.bidiws.dto.camion.CamionRequestDto;
import com.bidiws.dto.camion.CamionResponseDto;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Utilisateur;
import com.bidiws.entity.Ville;
import com.bidiws.enums.Role;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ChauffeurCamionRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifie l'isolation par ville sur les camions (meme principe que
 * ResidenceService) : une mairie ne doit voir/gerer que la flotte de sa
 * propre ville, pas celle des autres.
 */
@ExtendWith(MockitoExtension.class)
class CamionServiceTest {

    @Mock
    private CamionRepository camionRepository;
    @Mock
    private ChauffeurCamionRepository chauffeurCamionRepository;
    @Mock
    private VilleRepository villeRepository;

    @InjectMocks
    private CamionService camionService;

    private static final Long CAMION_ID = 1L;
    private static final Long VILLE_A_ID = 10L;
    private static final Long VILLE_B_ID = 20L;

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

    private Camion camionDansVille(Long villeId) {
        return Camion.builder()
                .id(CAMION_ID)
                .immatriculation("AB-123-CD")
                .actif(true)
                .ville(villeId != null ? Ville.builder().id(villeId).build() : null)
                .build();
    }

    private CamionRequestDto dtoPourVille(Long villeId) {
        return new CamionRequestDto("AB-123-CD", "Modele", "Benne", BigDecimal.TEN, false, false, villeId);
    }

    @Test
    void createEchoueSiLaMairieChoisitUneAutreVille() {
        CustomUserDetails mairieVilleA = utilisateur(2L, Role.MAIRIE, VILLE_A_ID);

        assertThatThrownBy(() -> camionService.create(dtoPourVille(VILLE_B_ID), mairieVilleA))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("propre ville");
    }

    @Test
    void createFonctionnePourLaMairieDeLaMemeVille() {
        CustomUserDetails mairieVilleA = utilisateur(2L, Role.MAIRIE, VILLE_A_ID);
        when(camionRepository.existsByImmatriculation("AB-123-CD")).thenReturn(false);
        when(villeRepository.findById(VILLE_A_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_A_ID).build()));
        when(camionRepository.save(any(Camion.class))).thenAnswer(inv -> inv.getArgument(0));

        CamionResponseDto result = camionService.create(dtoPourVille(VILLE_A_ID), mairieVilleA);

        assertThat(result.villeId()).isEqualTo(VILLE_A_ID);
    }

    @Test
    void createEchoueSiLimmatriculationExisteDeja() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        when(camionRepository.existsByImmatriculation("AB-123-CD")).thenReturn(true);

        assertThatThrownBy(() -> camionService.create(dtoPourVille(VILLE_A_ID), admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("immatriculation existe déjà");
    }

    @Test
    void updateEchoueSiLaMairieNeGerePasLaVilleDuCamion() {
        CustomUserDetails mairieVilleB = utilisateur(2L, Role.MAIRIE, VILLE_B_ID);
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionDansVille(VILLE_A_ID)));

        assertThatThrownBy(() -> camionService.update(CAMION_ID, dtoPourVille(VILLE_A_ID), mairieVilleB))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("votre ville");
    }

    @Test
    void adminAccedeAToutCamionQuelleQueSoitLaVille() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionDansVille(VILLE_B_ID)));
        when(villeRepository.findById(VILLE_B_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_B_ID).build()));
        when(camionRepository.save(any(Camion.class))).thenAnswer(inv -> inv.getArgument(0));

        CamionResponseDto result = camionService.update(CAMION_ID, dtoPourVille(VILLE_B_ID), admin);

        assertThat(result.villeId()).isEqualTo(VILLE_B_ID);
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> camionService.getById(CAMION_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Camion introuvable");
    }

    @Test
    void getAllPourUneMairieNeRetourneQueSaVille() {
        CustomUserDetails mairieVilleA = utilisateur(2L, Role.MAIRIE, VILLE_A_ID);
        when(camionRepository.findByVilleId(VILLE_A_ID)).thenReturn(List.of(camionDansVille(VILLE_A_ID)));

        assertThat(camionService.getAll(mairieVilleA, null)).hasSize(1);
    }

    @Test
    void getAllPourUneMairieSansVilleRattacheeEstVide() {
        CustomUserDetails mairieSansVille = utilisateur(2L, Role.MAIRIE, null);

        assertThat(camionService.getAll(mairieSansVille, null)).isEmpty();
    }

    @Test
    void getAllPourAdminRetourneTout() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        when(camionRepository.findAll()).thenReturn(List.of(camionDansVille(VILLE_A_ID), camionDansVille(VILLE_B_ID)));

        assertThat(camionService.getAll(admin, null)).hasSize(2);
    }

    @Test
    void getAllFiltreParActifApresLeScopingParVille() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        Camion actif = camionDansVille(VILLE_A_ID);
        Camion inactif = Camion.builder().id(2L).immatriculation("XY-999-ZZ").actif(false).ville(Ville.builder().id(VILLE_A_ID).build()).build();
        when(camionRepository.findAll()).thenReturn(List.of(actif, inactif));

        List<CamionResponseDto> result = camionService.getAll(admin, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actif()).isTrue();
    }

    @Test
    void desactiverEchoueSiLaMairieNeGerePasCeCamion() {
        CustomUserDetails mairieVilleB = utilisateur(2L, Role.MAIRIE, VILLE_B_ID);
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionDansVille(VILLE_A_ID)));

        assertThatThrownBy(() -> camionService.desactiver(CAMION_ID, mairieVilleB))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Accès non autorisé");
    }

    @Test
    void desactiverEchoueSiUnChauffeurEstAffecte() {
        CustomUserDetails admin = utilisateur(1L, Role.ADMIN, null);
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionDansVille(VILLE_A_ID)));
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID))
                .thenReturn(Optional.of(com.bidiws.entity.ChauffeurCamion.builder().build()));

        assertThatThrownBy(() -> camionService.desactiver(CAMION_ID, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("chauffeur est actuellement affecté");
    }
}
