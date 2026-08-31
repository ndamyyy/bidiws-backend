package com.bidiws.service;

import com.bidiws.dto.zone.ZoneRequestDto;
import com.bidiws.dto.zone.ZoneResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Ville;
import com.bidiws.entity.Zone;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Zone a une FK non couverte par le check residence existant :
 * tournee.zone_id (V1). Ajoute un check separe, meme pattern que
 * VilleService.delete.
 */
@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private VilleRepository villeRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private TourneeRepository tourneeRepository;

    @InjectMocks
    private ZoneService zoneService;

    private static final Long ZONE_ID = 1L;
    private static final Long VILLE_A_ID = 2L;
    private static final Long VILLE_B_ID = 3L;

    private Zone zone() {
        return Zone.builder().id(ZONE_ID).ville(Ville.builder().id(VILLE_A_ID).build()).nom("Secteur A").code("A").build();
    }

    private ZoneRequestDto dto(Long villeId) {
        return new ZoneRequestDto(villeId, "Secteur A", "A", null);
    }

    @Test
    void deleteEchoueSiIntrouvable() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.delete(ZONE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Zone introuvable");
    }

    @Test
    void deleteEchoueSiDesResidencesSontRattachees() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(residenceRepository.findByZoneId(ZONE_ID)).thenReturn(List.of(Residence.builder().id(9L).build()));

        assertThatThrownBy(() -> zoneService.delete(ZONE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidences sont rattachées");
    }

    @Test
    void deleteEchoueSiDesTourneesSontRattachees() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(residenceRepository.findByZoneId(ZONE_ID)).thenReturn(List.of());
        when(tourneeRepository.existsByZoneId(ZONE_ID)).thenReturn(true);

        assertThatThrownBy(() -> zoneService.delete(ZONE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tournées sont rattachées");
    }

    @Test
    void deleteReussitSiAucuneDependance() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(residenceRepository.findByZoneId(ZONE_ID)).thenReturn(List.of());
        when(tourneeRepository.existsByZoneId(ZONE_ID)).thenReturn(false);

        assertThatCode(() -> zoneService.delete(ZONE_ID)).doesNotThrowAnyException();

        verify(zoneRepository).deleteById(ZONE_ID);
    }

    @Test
    void updateChangeLibrementLeNomSansToucherLaVille() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(villeRepository.findById(VILLE_A_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_A_ID).build()));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        zoneService.update(ZONE_ID, dto(VILLE_A_ID));

        verify(residenceRepository, never()).findByZoneId(ZONE_ID);
        verify(tourneeRepository, never()).existsByZoneId(ZONE_ID);
    }

    @Test
    void updateEchoueSiChangementDeVilleAvecDesResidencesRattachees() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(villeRepository.findById(VILLE_B_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_B_ID).build()));
        when(residenceRepository.findByZoneId(ZONE_ID)).thenReturn(List.of(Residence.builder().id(9L).build()));

        assertThatThrownBy(() -> zoneService.update(ZONE_ID, dto(VILLE_B_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidences sont rattachées");
    }

    @Test
    void updateEchoueSiChangementDeVilleAvecDesTourneesRattachees() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(villeRepository.findById(VILLE_B_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_B_ID).build()));
        when(residenceRepository.findByZoneId(ZONE_ID)).thenReturn(List.of());
        when(tourneeRepository.existsByZoneId(ZONE_ID)).thenReturn(true);

        assertThatThrownBy(() -> zoneService.update(ZONE_ID, dto(VILLE_B_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tournées sont rattachées");
    }

    @Test
    void updateChangeLaVilleSiAucuneDependance() {
        when(zoneRepository.findById(ZONE_ID)).thenReturn(Optional.of(zone()));
        when(villeRepository.findById(VILLE_B_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_B_ID).build()));
        when(residenceRepository.findByZoneId(ZONE_ID)).thenReturn(List.of());
        when(tourneeRepository.existsByZoneId(ZONE_ID)).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoneResponseDto result = zoneService.update(ZONE_ID, dto(VILLE_B_ID));

        assertThat(result.villeId()).isEqualTo(VILLE_B_ID);
    }
}
