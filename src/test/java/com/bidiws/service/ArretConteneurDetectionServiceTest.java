package com.bidiws.service;

import com.bidiws.entity.ArretConteneur;
import com.bidiws.entity.Conteneur;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.repository.ArretConteneurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie que le premier capteur qui se declenche sur une residence a
 * plusieurs conteneurs ne confirme QUE son bac (pas tout l'arret), que
 * l'Arret parent n'est confirme que quand TOUS ses ArretConteneur le sont,
 * l'idempotence par bac (meme regle que ArretDetectionService.transitionAutorisee),
 * et la retrocompatibilite pour une residence sans conteneurs enregistres.
 */
@ExtendWith(MockitoExtension.class)
class ArretConteneurDetectionServiceTest {

    @Mock
    private ArretConteneurRepository arretConteneurRepository;
    @Mock
    private ArretDetectionService arretDetectionService;

    @InjectMocks
    private ArretConteneurDetectionService arretConteneurDetectionService;

    private static final Long ARRET_ID = 1L;
    private static final Long CONTENEUR_ID = 2L;
    private static final Long LIGNE_ID = 3L;

    private ArretConteneur ligne(StatutArret statut) {
        Conteneur conteneur = Conteneur.builder().id(CONTENEUR_ID).code("023").build();
        return ArretConteneur.builder().id(LIGNE_ID).conteneur(conteneur).statut(statut).build();
    }

    // ── Retrocompatibilite : residence sans conteneurs enregistres ──────

    @Test
    void residenceSansConteneursEnregistresAppliqueDirectementSurLArret() {
        when(arretConteneurRepository.findByArretIdAndConteneurId(ARRET_ID, CONTENEUR_ID)).thenReturn(Optional.empty());

        arretConteneurDetectionService.appliquerDetection(
                ARRET_ID, CONTENEUR_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);

        verify(arretDetectionService).appliquerDetection(
                ARRET_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);
        verify(arretConteneurRepository, never()).save(any());
    }

    // ── Idempotence par bac ──────────────────────────────────────────

    @Test
    void neFaitRienSiLeConteneurEstDejaConfirme() {
        when(arretConteneurRepository.findByArretIdAndConteneurId(ARRET_ID, CONTENEUR_ID))
                .thenReturn(Optional.of(ligne(StatutArret.COLLECTE_CONFIRMEE)));

        arretConteneurDetectionService.appliquerDetection(
                ARRET_ID, CONTENEUR_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);

        verify(arretConteneurRepository, never()).save(any());
        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void neFaitRienSiLeConteneurEstEnIncident() {
        when(arretConteneurRepository.findByArretIdAndConteneurId(ARRET_ID, CONTENEUR_ID))
                .thenReturn(Optional.of(ligne(StatutArret.INCIDENT)));

        arretConteneurDetectionService.appliquerDetection(
                ARRET_ID, CONTENEUR_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);

        verify(arretConteneurRepository, never()).save(any());
        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    // ── Agregation partielle / complete ───────────────────────────────

    @Test
    void confirmeUnBacSansToucherALArretSiDesAutresBacsRestentEnAttente() {
        ArretConteneur ligneCible = ligne(StatutArret.EN_ATTENTE);
        ArretConteneur autre1 = ligne(StatutArret.EN_ATTENTE);
        ArretConteneur autre2 = ligne(StatutArret.COLLECTE_CONFIRMEE);
        ArretConteneur autre3 = ligne(StatutArret.EN_ATTENTE);
        when(arretConteneurRepository.findByArretIdAndConteneurId(ARRET_ID, CONTENEUR_ID)).thenReturn(Optional.of(ligneCible));
        when(arretConteneurRepository.findByArretId(ARRET_ID))
                .thenReturn(List.of(ligneCible, autre1, autre2, autre3));

        arretConteneurDetectionService.appliquerDetection(
                ARRET_ID, CONTENEUR_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.RFID, (short) 90);

        assertThat(ligneCible.getStatut()).isEqualTo(StatutArret.COLLECTE_CONFIRMEE);
        assertThat(ligneCible.getHorodatageConfirmation()).isNotNull();
        verify(arretConteneurRepository).save(ligneCible);
        // 1/4 confirmes (ligneCible + autre2) : l'arret ne doit pas etre touche.
        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }

    @Test
    void confirmeLArretParentQuandLeDernierBacRestantEstConfirme() {
        ArretConteneur ligneCible = ligne(StatutArret.EN_ATTENTE);
        ArretConteneur dejaConfirme1 = ligne(StatutArret.COLLECTE_CONFIRMEE);
        ArretConteneur dejaConfirme2 = ligne(StatutArret.COLLECTE_CONFIRMEE);
        when(arretConteneurRepository.findByArretIdAndConteneurId(ARRET_ID, CONTENEUR_ID)).thenReturn(Optional.of(ligneCible));
        when(arretConteneurRepository.findByArretId(ARRET_ID))
                .thenReturn(List.of(ligneCible, dejaConfirme1, dejaConfirme2));

        arretConteneurDetectionService.appliquerDetection(
                ARRET_ID, CONTENEUR_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);

        verify(arretConteneurRepository).save(ligneCible);
        verify(arretDetectionService).appliquerDetection(
                ARRET_ID, StatutArret.COLLECTE_CONFIRMEE, ModeDetection.CAPTEUR_BENNE, (short) 90);
    }

    @Test
    void uneTransitionVersUnStatutNonConfirmeNeDeclenchePasLaVerificationDAgregation() {
        ArretConteneur ligneCible = ligne(StatutArret.EN_ATTENTE);
        when(arretConteneurRepository.findByArretIdAndConteneurId(ARRET_ID, CONTENEUR_ID)).thenReturn(Optional.of(ligneCible));

        arretConteneurDetectionService.appliquerDetection(
                ARRET_ID, CONTENEUR_ID, StatutArret.EN_APPROCHE, ModeDetection.GPS_AUTO, (short) 60);

        assertThat(ligneCible.getStatut()).isEqualTo(StatutArret.EN_APPROCHE);
        verify(arretConteneurRepository, never()).findByArretId(ARRET_ID);
        verify(arretDetectionService, never()).appliquerDetection(any(), any(), any(), any());
    }
}
