package com.bidiws.service;

import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.entity.PushToken;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Plateforme;
import com.bidiws.event.NotificationCreeeEvent;
import com.bidiws.repository.PushTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Best-effort strict : un echec FCM (token invalide, Firebase non
 * configure...) ne doit jamais faire echouer l'appelant ni interrompre
 * l'envoi aux autres tokens. Token definitivement invalide (UNREGISTERED/
 * INVALID_ARGUMENT) -> desactive ; erreur transitoire -> laisse actif.
 */
@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;
    @Mock
    private PushTokenRepository pushTokenRepository;
    @Mock
    private NotificationService notificationService;

    private static final Long DESTINATAIRE_ID = 1L;

    private PushToken pushToken(Long id) {
        return PushToken.builder()
                .id(id)
                .utilisateur(Utilisateur.builder().id(DESTINATAIRE_ID).build())
                .token("token-" + id)
                .plateforme(Plateforme.ANDROID)
                .actif(true)
                .build();
    }

    private FcmPushService service(boolean firebaseConfigure) {
        Optional<FirebaseMessaging> messaging = firebaseConfigure ? Optional.of(firebaseMessaging) : Optional.empty();
        return new FcmPushService(messaging, pushTokenRepository, notificationService);
    }

    @Test
    void neFaitRienSiFirebaseNonConfigure() {
        FcmPushService service = service(false);

        service.envoyerPush(DESTINATAIRE_ID, "Titre", "Message");

        verify(pushTokenRepository, never()).findByUtilisateurIdAndActifTrue(any());
    }

    @Test
    void envoieAChaqueTokenActifDuDestinataire() throws FirebaseMessagingException {
        FcmPushService service = service(true);
        PushToken token1 = pushToken(1L);
        PushToken token2 = pushToken(2L);
        when(pushTokenRepository.findByUtilisateurIdAndActifTrue(DESTINATAIRE_ID)).thenReturn(List.of(token1, token2));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        service.envoyerPush(DESTINATAIRE_ID, "Titre", "Message");

        verify(firebaseMessaging, times(2)).send(any(Message.class));
        verify(pushTokenRepository, never()).save(any());
    }

    @Test
    void tokenUnregisteredEstDesactive() throws FirebaseMessagingException {
        FcmPushService service = service(true);
        PushToken token = pushToken(1L);
        when(pushTokenRepository.findByUtilisateurIdAndActifTrue(DESTINATAIRE_ID)).thenReturn(List.of(token));
        FirebaseMessagingException exception = mockException(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        service.envoyerPush(DESTINATAIRE_ID, "Titre", "Message");

        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getActif()).isFalse();
    }

    @Test
    void tokenInvalidArgumentEstDesactive() throws FirebaseMessagingException {
        FcmPushService service = service(true);
        PushToken token = pushToken(1L);
        when(pushTokenRepository.findByUtilisateurIdAndActifTrue(DESTINATAIRE_ID)).thenReturn(List.of(token));
        FirebaseMessagingException exception = mockException(MessagingErrorCode.INVALID_ARGUMENT);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        service.envoyerPush(DESTINATAIRE_ID, "Titre", "Message");

        verify(pushTokenRepository).save(any());
    }

    @Test
    void erreurTransitoireNeDesactivePasLeToken() throws FirebaseMessagingException {
        FcmPushService service = service(true);
        PushToken token = pushToken(1L);
        when(pushTokenRepository.findByUtilisateurIdAndActifTrue(DESTINATAIRE_ID)).thenReturn(List.of(token));
        FirebaseMessagingException exception = mockException(MessagingErrorCode.UNAVAILABLE);
        when(exception.getMessage()).thenReturn("service indisponible");
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        service.envoyerPush(DESTINATAIRE_ID, "Titre", "Message");

        verify(pushTokenRepository, never()).save(any());
    }

    @Test
    void uneErreurInattendueSurUnTokenNinterrompPasLesAutres() throws FirebaseMessagingException {
        FcmPushService service = service(true);
        PushToken token1 = pushToken(1L);
        PushToken token2 = pushToken(2L);
        when(pushTokenRepository.findByUtilisateurIdAndActifTrue(DESTINATAIRE_ID)).thenReturn(List.of(token1, token2));
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(new RuntimeException("panne inattendue"))
                .thenReturn("message-id");

        service.envoyerPush(DESTINATAIRE_ID, "Titre", "Message");

        verify(firebaseMessaging, times(2)).send(any(Message.class));
    }

    @Test
    void surCreationNotificationRecupereLaNotifEtEnvoieLePush() throws FirebaseMessagingException {
        FcmPushService service = service(true);
        PushToken token = pushToken(1L);
        when(pushTokenRepository.findByUtilisateurIdAndActifTrue(DESTINATAIRE_ID)).thenReturn(List.of(token));
        when(notificationService.getById(5L)).thenReturn(new NotificationResponseDto(
                5L, DESTINATAIRE_ID, null, null, null, "Collecte terminée",
                "Les conteneurs peuvent être rentrés.", null, false, false, null, (LocalDateTime) null
        ));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        service.surCreationNotification(new NotificationCreeeEvent(5L, DESTINATAIRE_ID));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(captor.capture());
    }

    private FirebaseMessagingException mockException(MessagingErrorCode code) {
        FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        return exception;
    }
}
