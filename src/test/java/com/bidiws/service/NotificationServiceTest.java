package com.bidiws.service;

import com.bidiws.entity.Notification;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.TypeNotification;
import com.bidiws.event.NotificationCreeeEvent;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.NotificationRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ArretRepository arretRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long DESTINATAIRE_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    private Utilisateur destinataire() {
        return Utilisateur.builder().id(DESTINATAIRE_ID).nom("Diop").prenom("Amy").build();
    }

    private Notification notification() {
        return Notification.builder()
                .id(NOTIFICATION_ID)
                .destinataire(destinataire())
                .type(TypeNotification.COLLECTE_CONFIRMEE)
                .titre("Titre")
                .message("Message")
                .canal(CanalNotification.PUSH)
                .lu(false)
                .envoye(false)
                .build();
    }

    @Test
    void creerNotificationEchoueSiLeDestinataireEstIntrouvable() {
        when(utilisateurRepository.findById(DESTINATAIRE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.creerNotification(
                DESTINATAIRE_ID, null, null, TypeNotification.COLLECTE_CONFIRMEE, "T", "M", CanalNotification.PUSH))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Destinataire introuvable");
    }

    @Test
    void creerNotificationSauvegardeEtPublieUnEvent() {
        when(utilisateurRepository.findById(DESTINATAIRE_ID)).thenReturn(Optional.of(destinataire()));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(NOTIFICATION_ID);
            return n;
        });

        Notification result = notificationService.creerNotification(
                DESTINATAIRE_ID, null, null, TypeNotification.COLLECTE_CONFIRMEE, "Titre", "Message", null);

        assertThat(result.getId()).isEqualTo(NOTIFICATION_ID);
        assertThat(result.getCanal()).isEqualTo(CanalNotification.PUSH);

        ArgumentCaptor<NotificationCreeeEvent> captor = ArgumentCaptor.forClass(NotificationCreeeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().destinataireId()).isEqualTo(DESTINATAIRE_ID);
    }

    @Test
    void marquerLueEchoueSiLeDestinataireNestPasLePropretaire() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification()));

        assertThatThrownBy(() -> notificationService.marquerLue(NOTIFICATION_ID, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Accès non autorisé");
    }

    @Test
    void marquerLueFonctionnePourLeBonDestinataire() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification()));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = notificationService.marquerLue(NOTIFICATION_ID, DESTINATAIRE_ID);

        assertThat(result.lu()).isTrue();
        assertThat(result.heureLecture()).isNotNull();
    }

    @Test
    void marquerEnvoyeeMetAJourLeStatutEnvoye() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification()));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = notificationService.marquerEnvoyee(NOTIFICATION_ID);

        assertThat(result.envoye()).isTrue();
        assertThat(result.heureEnvoi()).isNotNull();
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getById(NOTIFICATION_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Notification introuvable");
    }

    @Test
    void getByDestinataireRetourneLaListe() {
        when(notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(DESTINATAIRE_ID))
                .thenReturn(List.of(notification()));

        assertThat(notificationService.getByDestinataire(DESTINATAIRE_ID)).hasSize(1);
    }

    @Test
    void getNonLuesRetourneLaListe() {
        when(notificationRepository.findByDestinataireIdAndLuFalse(DESTINATAIRE_ID))
                .thenReturn(List.of(notification()));

        assertThat(notificationService.getNonLues(DESTINATAIRE_ID)).hasSize(1);
    }

    @Test
    void countNonLuesRetourneLeCompteur() {
        when(notificationRepository.countByDestinataireIdAndLuFalse(DESTINATAIRE_ID)).thenReturn(3L);

        assertThat(notificationService.countNonLues(DESTINATAIRE_ID)).isEqualTo(3L);
    }
}
