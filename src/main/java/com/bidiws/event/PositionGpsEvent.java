package com.bidiws.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Publié à chaque nouveau signal GPS enregistré pour une tournée.
 * Consommé plus tard par la couche WebSocket pour la diffusion
 * temps réel de la position du camion (/topic/tournees/{tourneeId}/position).
 */
public record PositionGpsEvent(
        Long tourneeId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal vitesseKmh,
        LocalDateTime horodatage
){
}
