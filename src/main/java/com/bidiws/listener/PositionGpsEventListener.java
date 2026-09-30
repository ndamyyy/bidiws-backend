package com.bidiws.listener;

import com.bidiws.event.PositionGpsEvent;
import com.bidiws.service.GpsProximiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PositionGpsEventListener {

    private final GpsProximiteService gpsProximiteService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void detecterProximiteResidence(PositionGpsEvent event) {
        gpsProximiteService.detecterProximite(event);
    }
}
