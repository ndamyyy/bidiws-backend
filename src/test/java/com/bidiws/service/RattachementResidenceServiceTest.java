package com.bidiws.service;

import com.bidiws.entity.Residence;
import com.bidiws.entity.Ville;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.VilleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rattachement automatique a l'inscription (RegisterPage) : les 3
 * scenarios demandes explicitement — ville inconnue (aucune residence
 * creee), residence proche existante (rattachement, pas de doublon),
 * aucune residence proche (creation) — plus la deduplication GPS elle
 * meme (une residence a quelques centaines de metres n'est pas confondue
 * avec la meme residence).
 */
@ExtendWith(MockitoExtension.class)
class RattachementResidenceServiceTest {

    @Mock
    private VilleRepository villeRepository;
    @Mock
    private ResidenceRepository residenceRepository;

    @InjectMocks
    private RattachementResidenceService rattachementResidenceService;

    private static final Long VILLE_ID = 1L;
    private static final String VILLE_NOM = "Dakar";
    private static final BigDecimal LATITUDE = new BigDecimal("14.716677");
    private static final BigDecimal LONGITUDE = new BigDecimal("-17.467686");

    private Ville ville() {
        return Ville.builder().id(VILLE_ID).nom(VILLE_NOM).codePostal("10000").build();
    }

    @Test
    void aucuneResidenceCreeeSiLaVilleEstInconnue() {
        when(villeRepository.findByNomIgnoreCase("VilleInconnue")).thenReturn(Optional.empty());

        RattachementResidenceService.Resultat resultat = rattachementResidenceService.rattacher(
                "1 rue Inconnue", "99999", "VilleInconnue", LATITUDE, LONGITUDE);

        assertThat(resultat.zoneNonCouverte()).isTrue();
        assertThat(resultat.residence()).isNull();
        verify(residenceRepository, never()).findByVilleIdAndActifTrue(any());
        verify(residenceRepository, never()).save(any());
    }

    @Test
    void rattacheALaResidenceExistanteQuandElleEstAQuelquesMetres() {
        Residence residenceProche = Residence.builder()
                .id(5L).nom("Résidence Les Almadies").adresse("10 rue des Almadies")
                .codePostal("10000").ville(ville())
                // Meme coordonnees a un ecart infime (~1m) — doit matcher.
                .latitude(new BigDecimal("14.716680")).longitude(new BigDecimal("-17.467690"))
                .actif(true).build();

        when(villeRepository.findByNomIgnoreCase(VILLE_NOM)).thenReturn(Optional.of(ville()));
        when(residenceRepository.findByVilleIdAndActifTrue(VILLE_ID)).thenReturn(List.of(residenceProche));

        RattachementResidenceService.Resultat resultat = rattachementResidenceService.rattacher(
                "10 rue des Almadies", "10000", VILLE_NOM, LATITUDE, LONGITUDE);

        assertThat(resultat.zoneNonCouverte()).isFalse();
        assertThat(resultat.residence()).isEqualTo(residenceProche);
        // Pas de doublon : aucune nouvelle residence n'est creee.
        verify(residenceRepository, never()).save(any());
    }

    @Test
    void neConfondPasUneResidenceADesCentainesDeMetres() {
        // ~1,1 km au nord (0.01° de latitude) — meme ville, mais pas la
        // meme residence : le rayon de rattachement est volontairement
        // strict (15m), contrairement aux 50m de detection de proximite
        // camion (GpsProximityDetectionService).
        Residence residenceLointaine = Residence.builder()
                .id(6L).nom("Résidence Lointaine").adresse("Autre adresse")
                .codePostal("10000").ville(ville())
                .latitude(new BigDecimal("14.726677")).longitude(LONGITUDE)
                .actif(true).build();

        when(villeRepository.findByNomIgnoreCase(VILLE_NOM)).thenReturn(Optional.of(ville()));
        when(residenceRepository.findByVilleIdAndActifTrue(VILLE_ID)).thenReturn(List.of(residenceLointaine));
        when(residenceRepository.save(any(Residence.class))).thenAnswer(inv -> inv.getArgument(0));

        RattachementResidenceService.Resultat resultat = rattachementResidenceService.rattacher(
                "12 rue Neuve", "10000", VILLE_NOM, LATITUDE, LONGITUDE);

        assertThat(resultat.residence()).isNotEqualTo(residenceLointaine);
        assertThat(resultat.residence().getAdresse()).isEqualTo("12 rue Neuve");
    }

    @Test
    void creeUneNouvelleResidenceQuandAucuneNestProche() {
        when(villeRepository.findByNomIgnoreCase(VILLE_NOM)).thenReturn(Optional.of(ville()));
        when(residenceRepository.findByVilleIdAndActifTrue(VILLE_ID)).thenReturn(List.of());
        when(residenceRepository.save(any(Residence.class))).thenAnswer(inv -> inv.getArgument(0));

        RattachementResidenceService.Resultat resultat = rattachementResidenceService.rattacher(
                "27 avenue Cheikh Anta Diop", "10000", VILLE_NOM, LATITUDE, LONGITUDE);

        assertThat(resultat.zoneNonCouverte()).isFalse();

        ArgumentCaptor<Residence> captor = ArgumentCaptor.forClass(Residence.class);
        verify(residenceRepository).save(captor.capture());

        Residence creee = captor.getValue();
        assertThat(creee.getNom()).isEqualTo("27 avenue Cheikh Anta Diop");
        assertThat(creee.getAdresse()).isEqualTo("27 avenue Cheikh Anta Diop");
        assertThat(creee.getCodePostal()).isEqualTo("10000");
        assertThat(creee.getVille().getId()).isEqualTo(VILLE_ID);
        assertThat(creee.getLatitude()).isEqualTo(LATITUDE);
        assertThat(creee.getLongitude()).isEqualTo(LONGITUDE);
    }

    @Test
    void ignoreLesResidencesSansCoordonneesLorsDeLaRechercheDeProximite() {
        Residence sansCoordonnees = Residence.builder()
                .id(7L).nom("Résidence Sans GPS").adresse("Adresse")
                .codePostal("10000").ville(ville())
                .latitude(null).longitude(null)
                .actif(true).build();

        when(villeRepository.findByNomIgnoreCase(VILLE_NOM)).thenReturn(Optional.of(ville()));
        when(residenceRepository.findByVilleIdAndActifTrue(VILLE_ID)).thenReturn(List.of(sansCoordonnees));
        when(residenceRepository.save(any(Residence.class))).thenAnswer(inv -> inv.getArgument(0));

        RattachementResidenceService.Resultat resultat = rattachementResidenceService.rattacher(
                "12 rue Neuve", "10000", VILLE_NOM, LATITUDE, LONGITUDE);

        assertThat(resultat.residence()).isNotEqualTo(sansCoordonnees);
    }
}
