package com.bidiws.listener;

import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.event.ArretStatutChangeEvent;
import com.bidiws.event.NotificationCreeeEvent;
import com.bidiws.event.PositionGpsEvent;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Diffuse en temps reel, via STOMP, les events deja publies par les services
 * metier (ArretService, SignalGpsService, NotificationService). Le scoping
 * par residence/ville pour /topic/tournees/{id}(/position) est applique en
 * amont, a l'abonnement (StompAuthChannelInterceptor.authorizeSubscribe +
 * AuthorizationService.canAccessTournee) : ce listener publie sur le topic
 * sans avoir a filtrer par destinataire.
 *
 * AFTER_COMMIT : on ne diffuse que si la transaction a reellement abouti,
 * sinon un client verrait un changement qu'un rollback vient d'annuler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void diffuserChangementStatutArret(ArretStatutChangeEvent event) {
        messagingTemplate.convertAndSend("/topic/tournees/" + event.tourneeId(), event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void diffuserPositionGps(PositionGpsEvent event) {
        messagingTemplate.convertAndSend("/topic/tournees/" + event.tourneeId() + "/position", event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void diffuserNotification(NotificationCreeeEvent event) {
        Utilisateur destinataire = utilisateurRepository.findById(event.destinataireId()).orElse(null);
        if (destinataire == null) {
            log.warn("Notification {} : destinataire {} introuvable, diffusion WebSocket annulee",
                    event.notificationId(), event.destinataireId());
            return;
        }

        NotificationResponseDto payload = notificationService.getById(event.notificationId());
        messagingTemplate.convertAndSendToUser(destinataire.getEmail(), "/queue/notifications", payload);
    }
}
