package com.bidiws.service;

import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.event.PositionGpsEvent;
import com.bidiws.repository.ArretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie que la detection de proximite GPS ne fait jamais que
 * COLLECTE_PROBABLE (jamais CONFIRMEE seule), ignore les arrets deja a un
 * statut terminal ou deja probable, et calcule correctement la distance
 * par rapport a residence.rayonDetection.
 */
@ExtendWith(MockitoExtension.class)
class GpsProximiteServiceTest {

    @Mock
    private ArretRepository arretRepository;
    @Mock
    private ArretDetectionService arretDetectionService;

    @InjectMocks
    private GpsProximiteService gpsProximiteService;

    private static final Long TOURNEE_ID = 1L;
    private static final Long ARRET_ID = 2L;
    private static final BigDecimal LAT_RESIDENCE = BigDecimal.valueOf(14.7167);
    private static final BigDecimal LON_RESIDENCE = BigDecimal.valueOf(-17.4677);

    private Residence residence(BigDecimal latitude, BigDecimal longitude, Integer rayonDetection) {
        return Residence.builder()
                .id(3L)
                .latitude(latitude)
                .longitude(longitude)
                .rayonDetection(rayonDetection)
                .build();
    }

    private Arret arret(StatutArret statut, Residence residence) {
        return Arret.builder().id(ARRET_ID).statut(statut).residence(residence).build();
    }

    private PositionGpsEvent positionAuMemeEndroitQueLaResidence() {
        return new PositionGpsEvent(TOURNEE_ID, LAT_RESIDENCE, LON_RESIDENCE, BigDecimal.ZERO, LocalDateTime.now());
    }

    @Test
    void declencheCollecteProbableQuandLeCamionEstDansLeRayon() {
        Arret arret = arret(StatutArret.EN_ATTENTE, residence(LAT_RESIDENCE, LON_RESIDENCE, 50));
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService).appliquerDetection(ARRET_ID, StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, (short) 60);
    }

    @Test
    void neDeclenchePasSiLeCamionEstHorsDuRayon() {
        // ~111 m d'ecart en latitude (0.001 degre) pour un rayon de 50 m.
        Residence residenceLoin = residence(LAT_RESIDENCE.add(BigDecimal.valueOf(0.001)), LON_RESIDENCE, 50);
        Arret arret = arret(StatutArret.EN_ATTENTE, residenceLoin);
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void neDeclenchePasSiLarretEstDejaCollecteProbable() {
        Arret arret = arret(StatutArret.COLLECTE_PROBABLE, residence(LAT_RESIDENCE, LON_RESIDENCE, 50));
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void neDeclenchePasSiLarretEstDejaConfirme() {
        Arret arret = arret(StatutArret.COLLECTE_CONFIRMEE, residence(LAT_RESIDENCE, LON_RESIDENCE, 50));
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void neDeclenchePasSiLarretEstEnIncident() {
        Arret arret = arret(StatutArret.INCIDENT, residence(LAT_RESIDENCE, LON_RESIDENCE, 50));
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void ignoreUneResidenceSansCoordonnees() {
        Arret arret = arret(StatutArret.EN_ATTENTE, residence(null, null, 50));
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void continueDeTraiterLesAutresArretsSiUnAppelEchoue() {
        Arret arretEnErreur = Arret.builder().id(10L).statut(StatutArret.EN_ATTENTE)
                .residence(residence(LAT_RESIDENCE, LON_RESIDENCE, 50)).build();
        Arret arretOk = Arret.builder().id(11L).statut(StatutArret.EN_ATTENTE)
                .residence(residence(LAT_RESIDENCE, LON_RESIDENCE, 50)).build();
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arretEnErreur, arretOk));
        when(arretDetectionService.appliquerDetection(10L, StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, (short) 60))
                .thenThrow(new RuntimeException("erreur simulee"));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService).appliquerDetection(11L, StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, (short) 60);
    }

    @Test
    void traiteChaqueArretDeLaTourneeIndependamment() {
        Arret proche = arret(StatutArret.EN_ATTENTE, residence(LAT_RESIDENCE, LON_RESIDENCE, 50));
        Residence residenceLoin = residence(LAT_RESIDENCE.add(BigDecimal.valueOf(0.01)), LON_RESIDENCE, 50);
        Arret loin = Arret.builder().id(99L).statut(StatutArret.EN_ATTENTE).residence(residenceLoin).build();
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(proche, loin));

        gpsProximiteService.detecterProximite(positionAuMemeEndroitQueLaResidence());

        verify(arretDetectionService, times(1)).appliquerDetection(any(), any(), any(), any());
        verify(arretDetectionService).appliquerDetection(ARRET_ID, StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, (short) 60);
    }
}
