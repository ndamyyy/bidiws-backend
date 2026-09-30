package com.bidiws.service;

import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.TypeCollecteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TypeCollecte a deux FK reelles : calendrier_collecte.type_collecte_id
 * (deja verifie) et tournee.type_collecte_id (V1, non couvert avant —
 * remontait en 500 SQL brut au lieu d'un 409 propre).
 */
@ExtendWith(MockitoExtension.class)
class TypeCollecteServiceTest {

    @Mock
    private TypeCollecteRepository typeCollecteRepository;
    @Mock
    private CalendrierCollecteRepository calendrierCollecteRepository;
    @Mock
    private TourneeRepository tourneeRepository;

    @InjectMocks
    private TypeCollecteService typeCollecteService;

    private static final Long TYPE_COLLECTE_ID = 1L;

    private TypeCollecte typeCollecte() {
        return TypeCollecte.builder().id(TYPE_COLLECTE_ID).code("OM").libelle("Ordures ménagères").build();
    }

    @Test
    void deleteEchoueSiIntrouvable() {
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> typeCollecteService.delete(TYPE_COLLECTE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Type de collecte introuvable");
    }

    @Test
    void deleteEchoueSiUnCalendrierLutilise() {
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID))
                .thenReturn(List.of(CalendrierCollecte.builder().id(9L).build()));

        assertThatThrownBy(() -> typeCollecteService.delete(TYPE_COLLECTE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("utilisé dans un calendrier");
    }

    @Test
    void deleteEchoueSiUneTourneeLutilise() {
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID)).thenReturn(List.of());
        when(tourneeRepository.existsByTypeCollecteId(TYPE_COLLECTE_ID)).thenReturn(true);

        assertThatThrownBy(() -> typeCollecteService.delete(TYPE_COLLECTE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("utilisé par une tournée");
    }

    @Test
    void deleteReussitSiAucuneDependance() {
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID)).thenReturn(List.of());
        when(tourneeRepository.existsByTypeCollecteId(TYPE_COLLECTE_ID)).thenReturn(false);

        assertThatCode(() -> typeCollecteService.delete(TYPE_COLLECTE_ID)).doesNotThrowAnyException();

        verify(typeCollecteRepository).deleteById(TYPE_COLLECTE_ID);
    }
}
