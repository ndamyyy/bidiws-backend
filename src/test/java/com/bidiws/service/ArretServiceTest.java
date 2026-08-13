package com.bidiws.service;

import com.bidiws.dto.arret.ArretConteneurResponseDto;
import com.bidiws.dto.arret.ArretIncidentRequestDto;
import com.bidiws.dto.arret.ArretRequestDto;
import com.bidiws.dto.arret.ArretResponseDto;
import com.bidiws.entity.Arret;
import com.bidiws.entity.ArretConteneur;
import com.bidiws.entity.Conteneur;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Tournee;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.repository.ArretConteneurRepository;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TourneeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArretServiceTest {

    @Mock
    private ArretRepository arretRepository;
    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private ConteneurRepository conteneurRepository;
    @Mock
    private ArretConteneurRepository arretConteneurRepository;
    @Mock
    private ArretDetectionService arretDetectionService;

    @InjectMocks
    private ArretService arretService;

    private static final Long TOURNEE_ID = 10L;
    private static final Long RESIDENCE_ID = 20L;
    private static final Long ARRET_ID = 1L;

    private Tournee tournee() {
        return Tournee.builder().id(TOURNEE_ID).build();
    }

    private Residence residence() {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").build();
    }

    private Arret arret() {
        return Arret.builder()
                .id(ARRET_ID)
                .tournee(tournee())
                .residence(residence())
                .ordre((short) 1)
                .statut(StatutArret.EN_ATTENTE)
                .build();
    }

    @Test
    void creerEchoueSiLaTourneeEstIntrouvable() {
        ArretRequestDto dto = new ArretRequestDto(TOURNEE_ID, RESIDENCE_ID, 1, null, null);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> arretService.creer(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tournée introuvable");
    }

    @Test
    void creerEchoueSiLaResidenceEstIntrouvable() {
        ArretRequestDto dto = new ArretRequestDto(TOURNEE_ID, RESIDENCE_ID, 1, null, null);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee()));
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> arretService.creer(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Résidence introuvable");
    }

    @Test
    void creerSauvegardeLArretAvecStatutEnAttente() {
        ArretRequestDto dto = new ArretRequestDto(TOURNEE_ID, RESIDENCE_ID, 2, 3, "OM,verre");
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee()));
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence()));
        when(arretRepository.save(any(Arret.class))).thenAnswer(inv -> {
            Arret a = inv.getArgument(0);
            a.setId(ARRET_ID);
            return a;
        });
        when(conteneurRepository.findByResidenceIdAndActifTrue(RESIDENCE_ID)).thenReturn(List.of());

        ArretResponseDto result = arretService.creer(dto);

        assertThat(result.id()).isEqualTo(ARRET_ID);
        assertThat(result.statut()).isEqualTo(StatutArret.EN_ATTENTE);
        assertThat(result.ordre()).isEqualTo(2);
    }

    @Test
    void creerNeGenereAucuneLigneArretConteneurSiLaResidenceNaAucunConteneur() {
        ArretRequestDto dto = new ArretRequestDto(TOURNEE_ID, RESIDENCE_ID, 1, null, null);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee()));
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence()));
        when(arretRepository.save(any(Arret.class))).thenAnswer(inv -> {
            Arret a = inv.getArgument(0);
            a.setId(ARRET_ID);
            return a;
        });
        when(conteneurRepository.findByResidenceIdAndActifTrue(RESIDENCE_ID)).thenReturn(List.of());

        arretService.creer(dto);

        verify(arretConteneurRepository, never()).save(any(ArretConteneur.class));
    }

    @Test
    void creerGenereUneLigneArretConteneurEnAttentePourChaqueConteneurActifDeLaResidence() {
        ArretRequestDto dto = new ArretRequestDto(TOURNEE_ID, RESIDENCE_ID, 1, null, null);
        Conteneur c1 = Conteneur.builder().id(101L).code("023").residence(residence()).actif(true).build();
        Conteneur c2 = Conteneur.builder().id(102L).code("024").residence(residence()).actif(true).build();
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee()));
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence()));
        when(arretRepository.save(any(Arret.class))).thenAnswer(inv -> {
            Arret a = inv.getArgument(0);
            a.setId(ARRET_ID);
            return a;
        });
        when(conteneurRepository.findByResidenceIdAndActifTrue(RESIDENCE_ID)).thenReturn(List.of(c1, c2));
        when(arretConteneurRepository.save(any(ArretConteneur.class))).thenAnswer(inv -> inv.getArgument(0));

        arretService.creer(dto);

        ArgumentCaptor<ArretConteneur> captor = ArgumentCaptor.forClass(ArretConteneur.class);
        verify(arretConteneurRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ac -> ac.getConteneur().getId())
                .containsExactlyInAnyOrder(101L, 102L);
        assertThat(captor.getAllValues()).allSatisfy(ac -> assertThat(ac.getStatut()).isEqualTo(StatutArret.EN_ATTENTE));
    }

    @Test
    void changerStatutDelegueAuServiceDeDetectionAvecValidationChauffeur() {
        when(arretDetectionService.appliquerDetection(ARRET_ID, StatutArret.COLLECTE_CONFIRMEE,
                ModeDetection.VALIDATION_CHAUFFEUR, (short) 100)).thenReturn(arret());

        ArretResponseDto result = arretService.changerStatut(ARRET_ID, StatutArret.COLLECTE_CONFIRMEE);

        assertThat(result.id()).isEqualTo(ARRET_ID);
        verify(arretDetectionService).appliquerDetection(ARRET_ID, StatutArret.COLLECTE_CONFIRMEE,
                ModeDetection.VALIDATION_CHAUFFEUR, (short) 100);
    }

    @Test
    void signalerIncidentDelegueAuServiceDeDetection() {
        ArretIncidentRequestDto dto = new ArretIncidentRequestDto("Conteneur renversé", "http://photo");
        when(arretDetectionService.signalerIncident(ARRET_ID, dto.descriptionIncident(), dto.photoIncidentUrl()))
                .thenReturn(arret());

        ArretResponseDto result = arretService.signalerIncident(ARRET_ID, dto);

        assertThat(result.id()).isEqualTo(ARRET_ID);
        verify(arretDetectionService).signalerIncident(ARRET_ID, "Conteneur renversé", "http://photo");
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> arretService.getById(ARRET_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Arrêt introuvable");
    }

    @Test
    void getByIdRetourneLArret() {
        when(arretRepository.findById(ARRET_ID)).thenReturn(Optional.of(arret()));

        assertThat(arretService.getById(ARRET_ID).id()).isEqualTo(ARRET_ID);
    }

    @Test
    void getByTourneeRetourneLaListeTrieeParOrdre() {
        when(arretRepository.findByTourneeIdOrderByOrdreAsc(TOURNEE_ID)).thenReturn(List.of(arret()));

        List<ArretResponseDto> result = arretService.getByTournee(TOURNEE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tourneeId()).isEqualTo(TOURNEE_ID);
    }

    @Test
    void getByResidenceRetourneLaListe() {
        when(arretRepository.findByResidenceIdOrderByCreatedAtDesc(RESIDENCE_ID)).thenReturn(List.of(arret()));

        List<ArretResponseDto> result = arretService.getByResidence(RESIDENCE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).residenceId()).isEqualTo(RESIDENCE_ID);
    }

    @Test
    void getConteneursEchoueSiLArretEstIntrouvable() {
        when(arretRepository.existsById(ARRET_ID)).thenReturn(false);

        assertThatThrownBy(() -> arretService.getConteneurs(ARRET_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Arrêt introuvable");
    }

    @Test
    void getConteneursRetourneLeDetailParBac() {
        Conteneur conteneur = Conteneur.builder().id(101L).code("023").residence(residence())
                .niveauRemplissagePct((short) 80).build();
        ArretConteneur ligne = ArretConteneur.builder()
                .id(500L)
                .arret(arret())
                .conteneur(conteneur)
                .statut(StatutArret.COLLECTE_CONFIRMEE)
                .scoreConfiance((short) 90)
                .build();
        when(arretRepository.existsById(ARRET_ID)).thenReturn(true);
        when(arretConteneurRepository.findByArretId(ARRET_ID)).thenReturn(List.of(ligne));

        List<ArretConteneurResponseDto> result = arretService.getConteneurs(ARRET_ID);

        assertThat(result).hasSize(1);
        ArretConteneurResponseDto dto = result.get(0);
        assertThat(dto.conteneurId()).isEqualTo(101L);
        assertThat(dto.statut()).isEqualTo(StatutArret.COLLECTE_CONFIRMEE);
        assertThat(dto.niveauRemplissagePct()).isEqualTo(80);
    }
}
