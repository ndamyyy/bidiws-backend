package com.bidiws.service;

import com.bidiws.dto.signalgps.SignalGpsRequestDto;
import com.bidiws.dto.signalgps.SignalGpsResponseDto;
import com.bidiws.entity.SignalGps;
import com.bidiws.entity.Tournee;
import com.bidiws.enums.SourceGps;
import com.bidiws.event.PositionGpsEvent;
import com.bidiws.repository.SignalGpsRepository;
import com.bidiws.repository.TourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignalGpsService {

    private final SignalGpsRepository signalGpsRepository;
    private final TourneeRepository tourneeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GpsProximityDetectionService gpsProximityDetectionService;

    @Transactional
    public SignalGpsResponseDto enregistrer(SignalGpsRequestDto dto) {

        Tournee tournee = tourneeRepository.findById(dto.tourneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));

        SignalGps signal = SignalGps.builder()
                .tournee(tournee)
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .vitesseKmh(dto.vitesseKmh())
                .cap(dto.cap())
                .precisionM(dto.precisionM().shortValue())
                .horodatage(dto.horodatage())
                .source(dto.source() != null ? dto.source() : SourceGps.APP)
                .build();

        SignalGps saved = signalGpsRepository.save(signal);

        gpsProximityDetectionService.verifierProximite(
                saved.getTournee().getId(), saved.getLatitude(), saved.getLongitude());

        eventPublisher.publishEvent(new PositionGpsEvent(
                saved.getTournee().getId(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getVitesseKmh(),
                saved.getHorodatage()
        ));

        return toResponseDto(saved);
    }

    public SignalGpsResponseDto getDernierSignal(Long tourneeId) {
        return signalGpsRepository.findFirstByTourneeIdOrderByHorodatageDesc(tourneeId)
                .map(this::toResponseDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun signal GPS pour cette tournée"));
    }

    public Page<SignalGpsResponseDto> getTrajet(Long tourneeId, Pageable pageable) {
        return signalGpsRepository.findByTourneeIdOrderByHorodatageAsc(tourneeId, pageable)
                .map(this::toResponseDto);
    }

    public Page<SignalGpsResponseDto> getEntreDates(Long tourneeId, LocalDateTime debut, LocalDateTime fin, Pageable pageable) {
        return signalGpsRepository.findByTourneeIdAndHorodatageBetweenOrderByHorodatageAsc(tourneeId, debut, fin, pageable)
                .map(this::toResponseDto);
    }

    private SignalGpsResponseDto toResponseDto(SignalGps s) {
        return new SignalGpsResponseDto(
                s.getId(),
                s.getTournee().getId(),
                s.getLatitude(),
                s.getLongitude(),
                s.getVitesseKmh(),
                s.getCap(),
                s.getPrecisionM().intValue(),
                s.getHorodatage(),
                s.getSource()
        );
    }
}