package com.bidiws.service;

import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Tournee;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.StatutTournee;
import com.bidiws.event.ArretStatutChangeEvent;
import com.bidiws.repository.ArretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Point d'entree unique pour toute transition d'Arret : verifie les regles
 * de transition existantes (transitionAutorisee) et le nouveau garde-fou
 * sur le statut de la tournee parente — une tournee TERMINEE/ANNULEE bloque
 * toute modification (detection automatique comme signalement d'incident),
 * sans jamais lever d'exception (appele aussi bien par un chauffeur que par
 * un capteur/GPS automatique qui ne doit pas planter sur un etat obsolete).
 *
 * Les trois appelants d'appliquerDetection (ArretService.changerStatut,
 * ArretConteneurDetectionService, GpsProximityDetectionService) mockent
 * tous ArretDetectionService dans leurs propres tests : ce garde-fou se
 * propage donc a eux sans qu'ils aient besoin d'etre modifies ni testes
 * a nouveau pour ce cas.
 */
@ExtendWith(MockitoExtension.class)
class ArretDetectionServiceTest {

    @Mock
    private ArretRepository arretRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ArretDetectionService arretDetectionService;

    private static final Long ARRET_ID = 1L;
    private static final Long TOURNEE_ID = 2L;
    private static final Long RESIDENCE_ID = 3L;

    private Tournee tournee(StatutTournee statut) {
        return Tournee.builder().id(TOURNEE_ID).statut(statut).build();
    }

    private Residence residence() {
        return Residence.builder().id(RESIDENCE_ID).build();
    }

    private Arret arret(StatutArret statutArret, StatutTournee statutTournee) {
        return Arret.builder().id(ARRET_ID).tournee(tournee(statutTournee)).residence(residence())
                .statut(statutArret).build();
    }

    @Test
    void appliquerDetectionFonctionneNormalementSurUneTourneeEnCours() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.EN_COURS);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));
        when(arretRepository.save(any(Arret.class))).thenAnswer(inv -> inv.getArgument(0));

        Arret result = arretDetectionService.appliquerDetection(
                ARRET_ID, StatutArret.EN_APPROCHE, ModeDetection.GPS_AUTO, (short) 60);

        assertThat(result.getStatut()).isEqualTo(StatutArret.EN_APPROCHE);
        verify(arretRepository).save(arret);
        verify(eventPublisher).publishEvent(any(ArretStatutChangeEvent.class));
    }

    @Test
    void appliquerDetectionFonctionneNormalementSurUneTourneePlanifiee() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.PLANIFIEE);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));
        when(arretRepository.save(any(Arret.class))).thenAnswer(inv -> inv.getArgument(0));

        Arret result = arretDetectionService.appliquerDetection(
                ARRET_ID, StatutArret.EN_APPROCHE, ModeDetection.GPS_AUTO, (short) 60);

        assertThat(result.getStatut()).isEqualTo(StatutArret.EN_APPROCHE);
        verify(arretRepository).save(arret);
    }

    @Test
    void appliquerDetectionIgnoreSiLaTourneeEstAnnulee() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.ANNULEE);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));

        Arret result = arretDetectionService.appliquerDetection(
                ARRET_ID, StatutArret.EN_APPROCHE, ModeDetection.GPS_AUTO, (short) 60);

        assertThat(result.getStatut()).isEqualTo(StatutArret.EN_ATTENTE);
        verify(arretRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void appliquerDetectionIgnoreSiLaTourneeEstTerminee() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.TERMINEE);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));

        Arret result = arretDetectionService.appliquerDetection(
                ARRET_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);

        assertThat(result.getStatut()).isEqualTo(StatutArret.EN_ATTENTE);
        verify(arretRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void appliquerDetectionRejetteLeStatutIncidentDirectement() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.EN_COURS);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));

        assertThatThrownBy(() -> arretDetectionService.appliquerDetection(
                ARRET_ID, StatutArret.INCIDENT, ModeDetection.VALIDATION_CHAUFFEUR, (short) 100))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("signalement d'incident dédié");
    }

    @Test
    void appliquerDetectionNeRetrogradePasUnArretDejaConfirme() {
        Arret arret = arret(StatutArret.COLLECTE_CONFIRMEE, StatutTournee.EN_COURS);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));

        Arret result = arretDetectionService.appliquerDetection(
                ARRET_ID, StatutArret.EN_APPROCHE, ModeDetection.GPS_AUTO, (short) 60);

        assertThat(result.getStatut()).isEqualTo(StatutArret.COLLECTE_CONFIRMEE);
        verify(arretRepository, never()).save(any());
    }

    @Test
    void signalerIncidentFonctionneNormalementSurUneTourneeEnCours() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.EN_COURS);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));
        when(arretRepository.save(any(Arret.class))).thenAnswer(inv -> inv.getArgument(0));

        Arret result = arretDetectionService.signalerIncident(ARRET_ID, "Conteneur renversé", "http://photo");

        assertThat(result.getStatut()).isEqualTo(StatutArret.INCIDENT);
        verify(arretRepository).save(arret);
        verify(eventPublisher).publishEvent(any(ArretStatutChangeEvent.class));
    }

    @Test
    void signalerIncidentIgnoreSiLaTourneeEstAnnulee() {
        Arret arret = arret(StatutArret.COLLECTE_CONFIRMEE, StatutTournee.ANNULEE);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));

        Arret result = arretDetectionService.signalerIncident(ARRET_ID, "Conteneur renversé", "http://photo");

        assertThat(result.getStatut()).isEqualTo(StatutArret.COLLECTE_CONFIRMEE);
        verify(arretRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void signalerIncidentIgnoreSiLaTourneeEstTerminee() {
        Arret arret = arret(StatutArret.EN_ATTENTE, StatutTournee.TERMINEE);
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret));

        Arret result = arretDetectionService.signalerIncident(ARRET_ID, "Conteneur renversé", "http://photo");

        assertThat(result.getStatut()).isEqualTo(StatutArret.EN_ATTENTE);
        verify(arretRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
