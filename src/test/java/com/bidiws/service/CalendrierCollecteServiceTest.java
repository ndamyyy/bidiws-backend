package com.bidiws.service;

import com.bidiws.dto.calendriercollecte.CalendrierCollecteRequestDto;
import com.bidiws.dto.calendriercollecte.CalendrierCollecteResponseDto;
import com.bidiws.dto.calendriercollecte.ResidenceADesservirDto;
import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.Residence;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.entity.Zone;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TypeCollecteRepository;
import com.bidiws.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private CalendrierCollecteService calendrierCollecteService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long TYPE_COLLECTE_ID = 2L;
    private static final Long CALENDRIER_ID = 3L;
    private static final Long ZONE_ID = 4L;
    private static final Long AUTRE_ZONE_ID = 5L;

    // Lundi confirmé (jourSemaine = 1, convention ISO-8601 déjà utilisée par
    // CalendrierCollecte.jourSemaine).
    private static final LocalDate LUNDI = LocalDate.of(2024, 1, 1);

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

    // ── getResidencesADesservir (suggestion d'arrêts depuis le calendrier) ──

    private Zone zone(Long id) {
        return Zone.builder().id(id).nom("Zone Test").code("Z" + id).build();
    }

    private Residence residenceDansZone(Long zoneId, boolean actif) {
        return Residence.builder()
                .id(RESIDENCE_ID)
                .nom("Résidence Test")
                .actif(actif)
                .zone(zone(zoneId))
                .build();
    }

    private CalendrierCollecte calendrier(Residence residence, int jourSemaine, boolean actif) {
        return CalendrierCollecte.builder()
                .id(CALENDRIER_ID)
                .residence(residence)
                .typeCollecte(typeCollecte())
                .jourSemaine((short) jourSemaine)
                .actif(actif)
                .build();
    }

    private void stubZoneEtTypeCollecteValides() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone(ZONE_ID)));
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
    }

    @Test
    void getResidencesADesservirIncutLaResidenceBonJourEtBonneZone() {
        stubZoneEtTypeCollecteValides();
        Residence residence = residenceDansZone(ZONE_ID, true);
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID))
                .thenReturn(List.of(calendrier(residence, 1, true)));

        List<ResidenceADesservirDto> result =
                calendrierCollecteService.getResidencesADesservir(ZONE_ID, LUNDI, TYPE_COLLECTE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(RESIDENCE_ID);
    }

    @Test
    void getResidencesADesservirExclutUnMauvaisJour() {
        stubZoneEtTypeCollecteValides();
        Residence residence = residenceDansZone(ZONE_ID, true);
        // Calendrier pour mardi (2), on interroge pour lundi (1) : exclu.
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID))
                .thenReturn(List.of(calendrier(residence, 2, true)));

        List<ResidenceADesservirDto> result =
                calendrierCollecteService.getResidencesADesservir(ZONE_ID, LUNDI, TYPE_COLLECTE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getResidencesADesservirExclutUneMauvaiseZone() {
        stubZoneEtTypeCollecteValides();
        // Résidence dans une autre zone que celle demandée.
        Residence residence = residenceDansZone(AUTRE_ZONE_ID, true);
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID))
                .thenReturn(List.of(calendrier(residence, 1, true)));

        List<ResidenceADesservirDto> result =
                calendrierCollecteService.getResidencesADesservir(ZONE_ID, LUNDI, TYPE_COLLECTE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getResidencesADesservirExclutUnCalendrierInactif() {
        stubZoneEtTypeCollecteValides();
        Residence residence = residenceDansZone(ZONE_ID, true);
        when(calendrierCollecteRepository.findByTypeCollecteId(TYPE_COLLECTE_ID))
                .thenReturn(List.of(calendrier(residence, 1, false)));

        List<ResidenceADesservirDto> result =
                calendrierCollecteService.getResidencesADesservir(ZONE_ID, LUNDI, TYPE_COLLECTE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getResidencesADesservirEchoueSiLaZoneEstIntrouvable() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendrierCollecteService.getResidencesADesservir(ZONE_ID, LUNDI, TYPE_COLLECTE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Zone introuvable");
    }
}
