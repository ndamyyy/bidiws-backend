package com.bidiws.service;

import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.entity.PushToken;
import com.bidiws.event.NotificationCreeeEvent;
import com.bidiws.repository.PushTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

/**
 * Notifications push reelles via Firebase Cloud Messaging, en complement
 * du WebSocket existant (WebSocketBroadcastListener.diffuserNotification) :
 * le WebSocket couvre l'app ouverte au premier plan, FCM couvre l'app
 * fermee ou en arriere-plan. Les deux reagissent independamment au meme
 * NotificationCreeeEvent — coexistence, pas de remplacement.
 *
 * Best-effort strict, de bout en bout : Optional&lt;FirebaseMessaging&gt;
 * (voir FcmConfig — le bean n'existe pas tant que Firebase n'est pas
 * configure) et chaque envoi individuel isole dans son propre try/catch.
 * Un echec FCM (token invalide, quota, Firebase non configure...) ne fait
 * JAMAIS echouer la creation de la notification elle-meme — deja garanti
 * structurellement ici puisque cette methode reagit APRES coup
 * (AFTER_COMMIT, @Async) a une notification deja sauvegardee, mais les
 * try/catch internes evitent aussi qu'une exception ici pollue les logs
 * de facon incontrolee ou n'empeche l'envoi push aux autres tokens/utilisateurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final Optional<FirebaseMessaging> firebaseMessaging;
    private final PushTokenRepository pushTokenRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surCreationNotification(NotificationCreeeEvent event) {
        if (firebaseMessaging.isEmpty()) {
            log.debug("FCM non configure (bidiws.fcm.credentials-path absent) : "
                    + "push ignore pour la notification {}", event.notificationId());
            return;
        }

        NotificationResponseDto notif = notificationService.getById(event.notificationId());
        envoyerPush(event.destinataireId(), notif.titre(), notif.message());
    }

    // Point d'entree public (pas seulement le listener) : reutilisable
    // directement si un futur besoin (renvoi manuel, test admin...) doit
    // declencher un push sans repasser par la creation d'une Notification.
    public void envoyerPush(Long destinataireId, String titre, String message) {
        if (firebaseMessaging.isEmpty()) {
            return;
        }

        List<PushToken> tokens = pushTokenRepository.findByUtilisateurIdAndActifTrue(destinataireId);
        for (PushToken pushToken : tokens) {
            envoyerSurUnToken(firebaseMessaging.get(), pushToken, titre, message);
        }
    }

    private void envoyerSurUnToken(FirebaseMessaging messaging, PushToken pushToken, String titre, String message) {
        try {
            Message fcmMessage = Message.builder()
                    .setToken(pushToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(titre)
                            .setBody(message)
                            .build())
                    .build();

            messaging.send(fcmMessage);
        } catch (FirebaseMessagingException e) {
            if (estTokenDefinitivementInvalide(e)) {
                desactiverToken(pushToken);
            } else {
                log.warn("Echec envoi push FCM (token {}, utilisateur {}) : {}",
                        pushToken.getId(), pushToken.getUtilisateur().getId(), e.getMessage());
            }
        } catch (Exception e) {
            // Filet de securite ultime : best-effort veut dire best-effort,
            // meme une erreur totalement inattendue ici ne doit jamais
            // remonter ni interrompre l'envoi aux autres tokens.
            log.error("Erreur inattendue lors de l'envoi push FCM (token {})", pushToken.getId(), e);
        }
    }

    // UNREGISTERED : app desinstallee ou token expire/remplace cote client.
    // INVALID_ARGUMENT : token malforme (jamais valide, ex. tronque en base).
    // Dans les deux cas, reessayer plus tard ne changera rien — desactiver
    // evite de gaspiller un appel FCM a chaque notification future.
    // A l'inverse, QUOTA_EXCEEDED/UNAVAILABLE/INTERNAL sont transitoires :
    // le token reste valide, on ne fait que logger.
    private boolean estTokenDefinitivementInvalide(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    // Pas de @Transactional : sans effet sur une methode privee (Spring AOP
    // n'intercepte pas l'auto-invocation), et inutile de toute facon —
    // save() est individuellement transactionnel sur le repository
    // (comportement par defaut de Spring Data JPA), meme raisonnement que
    // AuthService.enregistrerEchec.
    private void desactiverToken(PushToken pushToken) {
        log.info("Push token {} invalide/expire (utilisateur {}), desactivation",
                pushToken.getId(), pushToken.getUtilisateur().getId());
        pushToken.setActif(false);
        pushTokenRepository.save(pushToken);
    }
}
