package com.bidiws.service;

import com.bidiws.entity.Ville;
import com.bidiws.entity.Zone;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ville a trois FK reelles (camion.ville_id, residence.ville_id,
 * utilisateur.ville_id — comptes MAIRIE) en plus des zones deja
 * verifiees. residence.zone_id etant nullable, une residence sans zone ne
 * serait pas bloquee par le seul check zones, d'ou un check residence
 * separe et direct sur ville_id.
 */
@ExtendWith(MockitoExtension.class)
class VilleServiceTest {

    @Mock
    private VilleRepository villeRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private CamionRepository camionRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ResidenceRepository residenceRepository;

    @InjectMocks
    private VilleService villeService;

    private static final Long VILLE_ID = 1L;

    private Ville ville() {
        return Ville.builder().id(VILLE_ID).nom("Étain").codePostal("55400").build();
    }

    @Test
    void deleteEchoueSiIntrouvable() {
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> villeService.delete(VILLE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ville introuvable");
    }

    @Test
    void deleteEchoueSiDesZonesSontRattachees() {
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.of(ville()));
        when(zoneRepository.findByVilleId(VILLE_ID)).thenReturn(List.of(Zone.builder().id(9L).build()));

        assertThatThrownBy(() -> villeService.delete(VILLE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("zones sont rattachées");
    }

    @Test
    void deleteEchoueSiDesResidencesSontRattachees() {
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.of(ville()));
        when(zoneRepository.findByVilleId(VILLE_ID)).thenReturn(List.of());
        when(residenceRepository.existsByVilleId(VILLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> villeService.delete(VILLE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidences sont rattachées");
    }

    @Test
    void deleteEchoueSiDesCamionsSontRattaches() {
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.of(ville()));
        when(zoneRepository.findByVilleId(VILLE_ID)).thenReturn(List.of());
        when(residenceRepository.existsByVilleId(VILLE_ID)).thenReturn(false);
        when(camionRepository.existsByVilleId(VILLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> villeService.delete(VILLE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("camions sont rattachés");
    }

    @Test
    void deleteEchoueSiDesUtilisateursSontRattaches() {
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.of(ville()));
        when(zoneRepository.findByVilleId(VILLE_ID)).thenReturn(List.of());
        when(residenceRepository.existsByVilleId(VILLE_ID)).thenReturn(false);
        when(camionRepository.existsByVilleId(VILLE_ID)).thenReturn(false);
        when(utilisateurRepository.existsByVilleId(VILLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> villeService.delete(VILLE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("comptes MAIRIE sont rattachés");
    }

    @Test
    void deleteReussitSiAucuneDependance() {
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.of(ville()));
        when(zoneRepository.findByVilleId(VILLE_ID)).thenReturn(List.of());
        when(residenceRepository.existsByVilleId(VILLE_ID)).thenReturn(false);
        when(camionRepository.existsByVilleId(VILLE_ID)).thenReturn(false);
        when(utilisateurRepository.existsByVilleId(VILLE_ID)).thenReturn(false);

        assertThatCode(() -> villeService.delete(VILLE_ID)).doesNotThrowAnyException();

        verify(villeRepository).deleteById(VILLE_ID);
    }
}
