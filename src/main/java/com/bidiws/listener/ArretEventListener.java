package com.bidiws.listener;

import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.TypeNotification;
import com.bidiws.event.ArretStatutChangeEvent;
import com.bidiws.service.NotificationService;
import com.bidiws.service.ResidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArretEventListener {

    private final NotificationService notificationService;
    private final ResidenceService residenceService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifierGardiensCollecteConfirmee(ArretStatutChangeEvent event) {

        if (event.nouveauStatut() != StatutArret.COLLECTE_CONFIRMEE) {
            return;
        }

        log.info(
                "Collecte confirmée par le résidence {}",
                event.residenceId()
        );

        var gardiens = residenceService.getGardiens(event.residenceId());

        for (var gardien : gardiens) {
            try {
                notificationService.creerNotification(
                        gardien.getId(),
                        event.arretId(),
                        event.residenceId(),
                        TypeNotification.COLLECTE_CONFIRMEE,
                        "Collecte terminée",
                        "Les conteneurs peuvent maintenant être rentrés.",
                        CanalNotification.PUSH
                );
            } catch (Exception ex) {
                log.error("Échec de la notification au gardien {} pour l'arrêt {}",
                        gardien.getId(), event.arretId(), ex);
            }
        }
    }
}
