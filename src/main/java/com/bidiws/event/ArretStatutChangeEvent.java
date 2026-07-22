package com.bidiws.event;

import com.bidiws.enums.StatutArret;

/**
 * Publié à chaque changement de statut d'un Arret.
 * Un futur @EventListener (couche WebSocket) s'en sert pour diffuser
 * en temps réel sur /topic/tournees/{tourneeId} sans que ArretService
 * ne dépende directement de STOMP.
 */
public record ArretStatutChangeEvent (
        Long arretId,
        Long tourneeId,
        Long residenceId,
        StatutArret ancienStatut,
        StatutArret nouveauStatut
){
}
