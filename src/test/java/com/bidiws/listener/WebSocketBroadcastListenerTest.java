package com.bidiws.listener;

import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.TypeNotification;
import com.bidiws.event.ArretStatutChangeEvent;
import com.bidiws.event.NotificationCreeeEvent;
import com.bidiws.event.PositionGpsEvent;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie que chaque event metier declenche bien l'envoi STOMP attendu,
 * sur la bonne destination et avec le bon payload.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketBroadcastListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private WebSocketBroadcastListener listener;

    @Test
    void diffuseLeChangementDeStatutSurLeTopicDeLaTournee() {
        ArretStatutChangeEvent event = new ArretStatutChangeEvent(
                1L, 42L, 7L, StatutArret.EN_ATTENTE, StatutArret.COLLECTE_CONFIRMEE);

        listener.diffuserChangementStatutArret(event);

        verify(messagingTemplate).convertAndSend("/topic/tournees/42", event);
    }

    @Test
    void diffuseLaPositionGpsSurLeTopicDePosition() {
        PositionGpsEvent event = new PositionGpsEvent(
                42L, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(30), LocalDateTime.now());

        listener.diffuserPositionGps(event);

        verify(messagingTemplate).convertAndSend("/topic/tournees/42/position", event);
    }

    @Test
    void diffuseLaNotificationUniquementAuDestinataire() {
        NotificationCreeeEvent event = new NotificationCreeeEvent(5L, 9L);
        Utilisateur destinataire = Utilisateur.builder().id(9L).email("gardien@bidiws.com").build();
        NotificationResponseDto dto = new NotificationResponseDto(
                5L, 9L, null, null, TypeNotification.COLLECTE_CONFIRMEE,
                "Titre", "Message", CanalNotification.PUSH, false, false, null, null);

        when(utilisateurRepository.findById(9L)).thenReturn(Optional.of(destinataire));
        when(notificationService.getById(5L)).thenReturn(dto);

        listener.diffuserNotification(event);

        verify(messagingTemplate).convertAndSendToUser("gardien@bidiws.com", "/queue/notifications", dto);
    }

    @Test
    void neDiffusePasSiLeDestinataireEstIntrouvable() {
        NotificationCreeeEvent event = new NotificationCreeeEvent(5L, 9L);
        when(utilisateurRepository.findById(9L)).thenReturn(Optional.empty());

        listener.diffuserNotification(event);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}
