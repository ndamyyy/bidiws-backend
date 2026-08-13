package com.bidiws.service;

import com.bidiws.dto.iot.DetectionIotRequestDto;
import com.bidiws.dto.iot.DetectionIotResponseDto;
import com.bidiws.entity.AppareilIot;
import com.bidiws.entity.Arret;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Conteneur;
import com.bidiws.entity.Residence;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.TypeAppareilIot;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ConteneurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie que la detection IoT passe bien par le point d'entree unique
 * ArretDetectionService.appliquerDetection (pas de logique de transition
 * dupliquee), et resout correctement le conteneur selon le scenario
 * materiel (capteur embarque vs lecteur RFID sur camion).
 */
@ExtendWith(MockitoExtension.class)
class IotDetectionServiceTest {

    @Mock
    private ArretRepository arretRepository;
    @Mock
    private ConteneurRepository conteneurRepository;
    @Mock
    private ArretDetectionService arretDetectionService;

    @InjectMocks
    private IotDetectionService iotDetectionService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long CONTENEUR_ID = 2L;
    private static final Long ARRET_ID = 3L;
    private static final Long APPAREIL_ID = 4L;

    private Residence residence() {
        return Residence.builder().id(RESIDENCE_ID).build();
    }

    private Conteneur conteneurActif() {
        return Conteneur.builder().id(CONTENEUR_ID).code("023").residence(residence()).actif(true).build();
    }

    private Arret arret(StatutArret statut) {
        return Arret.builder().id(ARRET_ID).residence(residence()).statut(statut).build();
    }

    private AppareilIot capteurEmbarque() {
        return AppareilIot.builder()
                .id(APPAREIL_ID)
                .typeAppareil(TypeAppareilIot.CAPTEUR_BENNE)
                .conteneur(conteneurActif())
                .build();
    }

    private AppareilIot lecteurSurCamion() {
        return AppareilIot.builder()
                .id(APPAREIL_ID)
                .typeAppareil(TypeAppareilIot.LECTEUR_RFID)
                .camion(Camion.builder().id(5L).build())
                .build();
    }

    @Test
    void capteurEmbarqueUtiliseDirectementSonConteneurSansTag() {
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), null);
        when(arretRepository.findByResidenceIdAndTournee_DateTournee(RESIDENCE_ID, LocalDate.now()))
                .thenReturn(List.of(arret(StatutArret.EN_ATTENTE)));
        when(arretDetectionService.appliquerDetection(ARRET_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90))
                .thenReturn(arret(StatutArret.COLLECTE_CONFIRMEE));

        DetectionIotResponseDto result = iotDetectionService.enregistrerDetection(capteurEmbarque(), dto);

        assertThat(result.arretId()).isEqualTo(ARRET_ID);
        assertThat(result.statut()).isEqualTo(StatutArret.COLLECTE_CONFIRMEE);
    }

    @Test
    void lecteurSurCamionEchoueSansRfidTag() {
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), null);

        assertThatThrownBy(() -> iotDetectionService.enregistrerDetection(lecteurSurCamion(), dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("rfidTag requis");
    }

    @Test
    void lecteurSurCamionResoutLeConteneurParLeTag() {
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), "TAG-023");
        when(conteneurRepository.findByRfidTag("TAG-023")).thenReturn(Optional.of(conteneurActif()));
        when(arretRepository.findByResidenceIdAndTournee_DateTournee(RESIDENCE_ID, LocalDate.now()))
                .thenReturn(List.of(arret(StatutArret.EN_ATTENTE)));
        when(arretDetectionService.appliquerDetection(ARRET_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.RFID, (short) 90))
                .thenReturn(arret(StatutArret.COLLECTE_CONFIRMEE));

        DetectionIotResponseDto result = iotDetectionService.enregistrerDetection(lecteurSurCamion(), dto);

        assertThat(result.arretId()).isEqualTo(ARRET_ID);
        verify(arretDetectionService).appliquerDetection(ARRET_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.RFID, (short) 90);
    }

    @Test
    void echoueSiAucunTagNeCorrespond() {
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), "TAG-INCONNU");
        when(conteneurRepository.findByRfidTag("TAG-INCONNU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> iotDetectionService.enregistrerDetection(lecteurSurCamion(), dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun conteneur");
    }

    @Test
    void echoueSiLeConteneurEstInactif() {
        Conteneur inactif = Conteneur.builder().id(CONTENEUR_ID).code("023").residence(residence()).actif(false).build();
        AppareilIot appareil = AppareilIot.builder().id(APPAREIL_ID).typeAppareil(TypeAppareilIot.CAPTEUR_BENNE).conteneur(inactif).build();
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), null);

        assertThatThrownBy(() -> iotDetectionService.enregistrerDetection(appareil, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inactif");
    }

    @Test
    void echoueSiAucunArretPrevuAujourdhui() {
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), null);
        when(arretRepository.findByResidenceIdAndTournee_DateTournee(RESIDENCE_ID, LocalDate.now())).thenReturn(List.of());

        assertThatThrownBy(() -> iotDetectionService.enregistrerDetection(capteurEmbarque(), dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun arrêt prévu");
    }

    @Test
    void appareilSansConteneurNiCamionEstRefuse() {
        AppareilIot appareilOrphelin = AppareilIot.builder().id(APPAREIL_ID).typeAppareil(TypeAppareilIot.CAPTEUR_BENNE).build();
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), null);

        assertThatThrownBy(() -> iotDetectionService.enregistrerDetection(appareilOrphelin, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'est rattaché à aucun");
    }

    @Test
    void preferentUnArretPasEncoreTerminalSiPlusieursExistent() {
        Arret dejaConfirme = arret(StatutArret.COLLECTE_CONFIRMEE);
        Arret enAttente = Arret.builder().id(99L).residence(residence()).statut(StatutArret.EN_ATTENTE).build();
        DetectionIotRequestDto dto = new DetectionIotRequestDto(LocalDateTime.now(), null);
        when(arretRepository.findByResidenceIdAndTournee_DateTournee(RESIDENCE_ID, LocalDate.now()))
                .thenReturn(List.of(dejaConfirme, enAttente));
        when(arretDetectionService.appliquerDetection(eq(99L), eq(StatutArret.COLLECTE_CONFIRMEE), eq(ModeDetection.CAPTEUR_BENNE), eq((short) 90)))
                .thenReturn(enAttente);

        iotDetectionService.enregistrerDetection(capteurEmbarque(), dto);

        verify(arretDetectionService).appliquerDetection(99L, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);
    }
}
