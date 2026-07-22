package com.bidiws.event;

/**
 * Publié après la persistance d'une Notification. Consommé plus tard par :
 * - la couche WebSocket (push temps réel vers /queue/users/{userId}/notifications)
 * - un futur service d'envoi SMS/EMAIL/PUSH selon le canal choisi
 */
public record NotificationCreeeEvent(
        Long notificationId,
        Long destinataireId
){
}
