package com.bidiws.service;

import com.bidiws.dto.signalement.SignalementRequestDto;
import com.bidiws.dto.signalement.SignalementResponseDto;
import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Signalement;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.StatutSignalement;
import com.bidiws.enums.TypeSignalement;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.SignalementRepository;
import com.bidiws.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalementServiceTest {

    @Mock
    private SignalementRepository signalementRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private ArretRepository arretRepository;

    @InjectMocks
    private SignalementService signalementService;

    private static final Long AUTEUR_ID = 1L;
    private static final Long RESIDENCE_ID = 2L;
    private static final Long SIGNALEMENT_ID = 3L;
    private static final Long GARDIEN_ID = 4L;
    private static final Long VILLE_ID = 5L;

    private Utilisateur auteur() {
        return Utilisateur.builder().id(AUTEUR_ID).nom("Diop").prenom("Amy").build();
    }

    private Residence residence() {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").build();
    }

    private Signalement signalement(StatutSignalement statut) {
        return Signalement.builder()
                .id(SIGNALEMENT_ID)
                .auteur(auteur())
                .residence(residence())
                .type(TypeSignalement.BAC_PLEIN)
                .statut(statut)
                .build();
    }

    @Test
    void createEchoueSiLauteurEstIntrouvable() {
        SignalementRequestDto dto = new SignalementRequestDto(RESIDENCE_ID, null, TypeSignalement.BAC_PLEIN, "desc", null, null, null);
        when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> signalementService.create(AUTEUR_ID, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Auteur introuvable");
    }

    @Test
    void createSauvegardeLeSignalementAvecStatutOuvert() {
        SignalementRequestDto dto = new SignalementRequestDto(RESIDENCE_ID, null, TypeSignalement.DEPOT_SAUVAGE, "desc", null, null, null);
        when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(auteur()));
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence()));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> {
            Signalement s = inv.getArgument(0);
            s.setId(SIGNALEMENT_ID);
            return s;
        });

        SignalementResponseDto result = signalementService.create(AUTEUR_ID, dto);

        assertThat(result.statut()).isEqualTo(StatutSignalement.OUVERT);
        assertThat(result.auteurId()).isEqualTo(AUTEUR_ID);
    }

    @Test
    void changerStatutVersResoluRenseigneResoluAt() {
        when(signalementRepository.findById(SIGNALEMENT_ID)).thenReturn(Optional.of(signalement(StatutSignalement.OUVERT)));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDto result = signalementService.changerStatut(SIGNALEMENT_ID, StatutSignalement.RESOLU);

        assertThat(result.statut()).isEqualTo(StatutSignalement.RESOLU);
        assertThat(result.resoluAt()).isNotNull();
    }

    @Test
    void changerStatutVersEnTraitementNeRenseignePasResoluAt() {
        when(signalementRepository.findById(SIGNALEMENT_ID)).thenReturn(Optional.of(signalement(StatutSignalement.OUVERT)));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDto result = signalementService.changerStatut(SIGNALEMENT_ID, StatutSignalement.EN_TRAITEMENT);

        assertThat(result.statut()).isEqualTo(StatutSignalement.EN_TRAITEMENT);
        assertThat(result.resoluAt()).isNull();
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(signalementRepository.findById(SIGNALEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> signalementService.getById(SIGNALEMENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Signalement introuvable");
    }

    @Test
    void getByAuteurRetourneLaListe() {
        when(signalementRepository.findByAuteurIdOrderByCreatedAtDesc(AUTEUR_ID))
                .thenReturn(List.of(signalement(StatutSignalement.OUVERT)));

        assertThat(signalementService.getByAuteur(AUTEUR_ID)).hasSize(1);
    }

    @Test
    void getByStatutRetourneLaListeNonScopee() {
        when(signalementRepository.findByStatutOrderByCreatedAtAsc(StatutSignalement.OUVERT))
                .thenReturn(List.of(signalement(StatutSignalement.OUVERT)));

        assertThat(signalementService.getByStatut(StatutSignalement.OUVERT)).hasSize(1);
    }

    @Test
    void getByStatutPourGardienNeRetourneQueSaResidence() {
        when(signalementRepository.findByStatutAndResidence_Gardiens_Gardien_IdOrderByCreatedAtAsc(
                StatutSignalement.OUVERT, GARDIEN_ID))
                .thenReturn(List.of(signalement(StatutSignalement.OUVERT)));

        assertThat(signalementService.getByStatutPourGardien(StatutSignalement.OUVERT, GARDIEN_ID)).hasSize(1);
    }

    @Test
    void getByStatutPourMairieNeRetourneQueSaVille() {
        when(signalementRepository.findByStatutAndResidence_Ville_IdOrderByCreatedAtAsc(
                StatutSignalement.OUVERT, VILLE_ID))
                .thenReturn(List.of(signalement(StatutSignalement.OUVERT)));

        assertThat(signalementService.getByStatutPourMairie(StatutSignalement.OUVERT, VILLE_ID)).hasSize(1);
    }
}
