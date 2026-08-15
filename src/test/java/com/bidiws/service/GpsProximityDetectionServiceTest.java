package com.bidiws.service;

import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.repository.ArretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie la detection automatique par proximite GPS : signal faible
 * (COLLECTE_PROBABLE / GPS_AUTO, jamais COLLECTE_CONFIRMEE), rayon par
 * residence avec defaut a 50m, skip silencieux sans coordonnees, et
 * filtrage des arrets deja traites via la requete EN_ATTENTE.
 */
@ExtendWith(MockitoExtension.class)
class GpsProximityDetectionServiceTest {

    @Mock
    private ArretRepository arretRepository;
    @Mock
    private ArretDetectionService arretDetectionService;

    @InjectMocks
    private GpsProximityDetectionService gpsProximityDetectionService;

    private static final Long TOURNEE_ID = 1L;
    private static final Long ARRET_ID = 2L;
    private static final Long RESIDENCE_ID = 3L;
    private static final BigDecimal POSITION_CAMION_LAT = BigDecimal.valueOf(14.7167);
    private static final BigDecimal POSITION_CAMION_LON = BigDecimal.valueOf(-17.4677);

    private Residence residence(BigDecimal latitude, BigDecimal longitude, Integer rayonDetection) {
        return Residence.builder().id(RESIDENCE_ID).latitude(latitude).longitude(longitude)
                .rayonDetection(rayonDetection).build();
    }

    private Arret arret(Residence residence) {
        return Arret.builder().id(ARRET_ID).residence(residence).statut(StatutArret.EN_ATTENTE).build();
    }

    @Test
    void distanceSousLeRayonPoseCollecteProbableAvecGpsAuto() {
        // Meme position que le camion : distance ~0m.
        Residence residence = residence(POSITION_CAMION_LAT, POSITION_CAMION_LON, 100);
        Arret arret = arret(residence);
        when(arretRepository.findByTourneeIdAndStatut(TOURNEE_ID, StatutArret.EN_ATTENTE)).thenReturn(List.of(arret));

        gpsProximityDetectionService.verifierProximite(TOURNEE_ID, POSITION_CAMION_LAT, POSITION_CAMION_LON);

        verify(arretDetectionService).appliquerDetection(
                ARRET_ID, StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, (short) 50);
    }

    @Test
    void distanceAuDessusDuRayonNeDeclencheRien() {
        // Decalage d'environ 111m en latitude, rayon explicite de 10m.
        Residence residence = residence(BigDecimal.valueOf(14.7177), POSITION_CAMION_LON, 10);
        Arret arret = arret(residence);
        when(arretRepository.findByTourneeIdAndStatut(TOURNEE_ID, StatutArret.EN_ATTENTE)).thenReturn(List.of(arret));

        gpsProximityDetectionService.verifierProximite(TOURNEE_ID, POSITION_CAMION_LAT, POSITION_CAMION_LON);

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void residenceSansCoordonneesEstIgnoreeSansException() {
        Residence residence = residence(null, null, 50);
        Arret arret = arret(residence);
        when(arretRepository.findByTourneeIdAndStatut(TOURNEE_ID, StatutArret.EN_ATTENTE)).thenReturn(List.of(arret));

        gpsProximityDetectionService.verifierProximite(TOURNEE_ID, POSITION_CAMION_LAT, POSITION_CAMION_LON);

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void arretDejaTraiteNEstPasReDeclencheCarFiltreEnAttente() {
        // Un arret deja en COLLECTE_PROBABLE (ou au-dela) n'est plus retourne
        // par findByTourneeIdAndStatut(..., EN_ATTENTE) : le filtre repository
        // suffit, aucun calcul de distance n'est meme tente.
        when(arretRepository.findByTourneeIdAndStatut(TOURNEE_ID, StatutArret.EN_ATTENTE)).thenReturn(List.of());

        gpsProximityDetectionService.verifierProximite(TOURNEE_ID, POSITION_CAMION_LAT, POSITION_CAMION_LON);

        verify(arretRepository).findByTourneeIdAndStatut(TOURNEE_ID, StatutArret.EN_ATTENTE);
        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void rayonDetectionNullUtiliseLeDefautDe50Metres() {
        // Decalage d'environ 34m en latitude : sous le defaut de 50m.
        Residence residence = residence(BigDecimal.valueOf(14.71701), POSITION_CAMION_LON, null);
        Arret arret = arret(residence);
        when(arretRepository.findByTourneeIdAndStatut(TOURNEE_ID, StatutArret.EN_ATTENTE)).thenReturn(List.of(arret));

        gpsProximityDetectionService.verifierProximite(TOURNEE_ID, POSITION_CAMION_LAT, POSITION_CAMION_LON);

        verify(arretDetectionService).appliquerDetection(
                ARRET_ID, StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, (short) 50);
    }
}
