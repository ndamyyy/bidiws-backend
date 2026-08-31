package com.bidiws.service;

import com.bidiws.dto.calendriercollecte.CalendrierCollecteRequestDto;
import com.bidiws.dto.calendriercollecte.CalendrierCollecteResponseDto;
import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.Residence;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TypeCollecteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ResidenceService.desactiver n'invalide jamais les calendriers deja crees
 * (residence.actif n'etait lu nulle part avant) — ici on verifie
 * uniquement que la CREATION d'un nouveau calendrier est bloquee sur une
 * residence desactivee, pas les calendriers existants.
 */
@ExtendWith(MockitoExtension.class)
class CalendrierCollecteServiceTest {

    @Mock
    private CalendrierCollecteRepository calendrierCollecteRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private TypeCollecteRepository typeCollecteRepository;

    @InjectMocks
    private CalendrierCollecteService calendrierCollecteService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long TYPE_COLLECTE_ID = 2L;
    private static final Long CALENDRIER_ID = 3L;

    private Residence residence(boolean actif) {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").actif(actif).build();
    }

    private TypeCollecte typeCollecte() {
        return TypeCollecte.builder().id(TYPE_COLLECTE_ID).code("OM").libelle("Ordures ménagères").build();
    }

    private CalendrierCollecteRequestDto dto() {
        return new CalendrierCollecteRequestDto(RESIDENCE_ID, TYPE_COLLECTE_ID, 1, LocalTime.of(8, 0));
    }

    @Test
    void createEchoueSiLaResidenceEstDesactivee() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(false)));

        assertThatThrownBy(() -> calendrierCollecteService.create(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidence est désactivée");
    }

    @Test
    void createFonctionnePourUneResidenceActive() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(true)));
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(calendrierCollecteRepository.save(any(CalendrierCollecte.class))).thenAnswer(inv -> {
            CalendrierCollecte c = inv.getArgument(0);
            c.setId(CALENDRIER_ID);
            return c;
        });

        CalendrierCollecteResponseDto result = calendrierCollecteService.create(dto());

        assertThat(result.id()).isEqualTo(CALENDRIER_ID);
        assertThat(result.residenceId()).isEqualTo(RESIDENCE_ID);
    }
}
