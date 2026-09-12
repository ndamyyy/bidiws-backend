package com.bidiws.service;

import com.bidiws.dto.signalgps.SignalGpsRequestDto;
import com.bidiws.dto.signalgps.SignalGpsResponseDto;
import com.bidiws.entity.SignalGps;
import com.bidiws.entity.Tournee;
import com.bidiws.enums.StatutTournee;
import com.bidiws.event.PositionGpsEvent;
import com.bidiws.repository.SignalGpsRepository;
import com.bidiws.repository.TourneeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalGpsServiceTest {

    @Mock
    private SignalGpsRepository signalGpsRepository;
    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private GpsProximityDetectionService gpsProximityDetectionService;

    @InjectMocks
    private SignalGpsService signalGpsService;

    private static final Long TOURNEE_ID = 1L;

    private Tournee tournee() {
        return Tournee.builder().id(TOURNEE_ID).build();
    }

    private Tournee tournee(StatutTournee statut) {
        return Tournee.builder().id(TOURNEE_ID).statut(statut).build();
    }

    private SignalGps signal(Long id) {
        return SignalGps.builder()
                .id(id)
                .tournee(tournee())
                .latitude(BigDecimal.valueOf(14.7167))
                .longitude(BigDecimal.valueOf(-17.4677))
                .vitesseKmh(BigDecimal.valueOf(32))
                .precisionM((short) 5)
                .horodatage(LocalDateTime.of(2026, 8, 1, 10, 0))
                .build();
    }

    @Test
    void enregistrerEchoueSiLaTourneeEstIntrouvable() {
        SignalGpsRequestDto dto = new SignalGpsRequestDto(
                TOURNEE_ID, BigDecimal.ONE, BigDecimal.ONE, null, null, 5, LocalDateTime.now(), null);
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> signalGpsService.enregistrer(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tournée introuvable");
    }

    @Test
    void enregistrerSauvegardeEtPublieUnPositionGpsEvent() {
        SignalGpsRequestDto dto = new SignalGpsRequestDto(
                TOURNEE_ID, BigDecimal.valueOf(14.71), BigDecimal.valueOf(-17.46),
                BigDecimal.valueOf(40), null, 5, LocalDateTime.of(2026, 8, 1, 10, 0), null);

        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee()));
        when(signalGpsRepository.save(any(SignalGps.class))).thenAnswer(inv -> {
            SignalGps s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        SignalGpsResponseDto result = signalGpsService.enregistrer(dto);

        assertThat(result.tourneeId()).isEqualTo(TOURNEE_ID);

        ArgumentCaptor<PositionGpsEvent> captor = ArgumentCaptor.forClass(PositionGpsEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tourneeId()).isEqualTo(TOURNEE_ID);

        verify(gpsProximityDetectionService).verifierProximite(
                TOURNEE_ID, BigDecimal.valueOf(14.71), BigDecimal.valueOf(-17.46));
    }

    @Test
    void enregistrerSauvegardeLeSignalMaisIgnoreProximiteEtEventSiLaTourneeEstAnnulee() {
        SignalGpsRequestDto dto = new SignalGpsRequestDto(
                TOURNEE_ID, BigDecimal.valueOf(14.71), BigDecimal.valueOf(-17.46),
                BigDecimal.valueOf(40), null, 5, LocalDateTime.of(2026, 8, 1, 10, 0), null);

        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.ANNULEE)));
        when(signalGpsRepository.save(any(SignalGps.class))).thenAnswer(inv -> {
            SignalGps s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        SignalGpsResponseDto result = signalGpsService.enregistrer(dto);

        assertThat(result.tourneeId()).isEqualTo(TOURNEE_ID);
        verify(gpsProximityDetectionService, never()).verifierProximite(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getDernierSignalEchoueSiAucunSignal() {
        when(signalGpsRepository.findFirstByTourneeIdOrderByHorodatageDesc(TOURNEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> signalGpsService.getDernierSignal(TOURNEE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun signal GPS");
    }

    @Test
    void getDernierSignalRetourneLeSignal() {
        when(signalGpsRepository.findFirstByTourneeIdOrderByHorodatageDesc(TOURNEE_ID))
                .thenReturn(Optional.of(signal(1L)));

        assertThat(signalGpsService.getDernierSignal(TOURNEE_ID).id()).isEqualTo(1L);
    }

    @Test
    void getTrajetRetourneUnePageDeSignaux() {
        Pageable pageable = PageRequest.of(0, 200);
        Page<SignalGps> page = new PageImpl<>(List.of(signal(1L), signal(2L)), pageable, 2);
        when(signalGpsRepository.findByTourneeIdOrderByHorodatageAsc(TOURNEE_ID, pageable)).thenReturn(page);

        Page<SignalGpsResponseDto> result = signalGpsService.getTrajet(TOURNEE_ID, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getEntreDatesRetourneUnePageDeSignaux() {
        LocalDateTime debut = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 8, 1, 23, 59);
        Pageable pageable = PageRequest.of(0, 200);
        Page<SignalGps> page = new PageImpl<>(List.of(signal(1L)), pageable, 1);
        when(signalGpsRepository.findByTourneeIdAndHorodatageBetweenOrderByHorodatageAsc(TOURNEE_ID, debut, fin, pageable))
                .thenReturn(page);

        Page<SignalGpsResponseDto> result = signalGpsService.getEntreDates(TOURNEE_ID, debut, fin, pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
