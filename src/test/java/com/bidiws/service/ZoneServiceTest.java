package com.bidiws.service;

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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Zone zone() {
        return Zone.builder().id(ZONE_ID).ville(Ville.builder().id(2L).build()).nom("Secteur A").code("A").build();
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
}
