package com.bidiws.service;

import com.bidiws.dto.residencegardien.ResidenceGardienRequestDto;
import com.bidiws.dto.residencegardien.ResidenceGardienResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.ResidenceGardien;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.ResidenceGardienRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResidenceGardienServiceTest {

    @Mock
    private ResidenceGardienRepository residenceGardienRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ResidenceGardienService residenceGardienService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long GARDIEN_ID = 2L;

    private Residence residence(boolean actif) {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").actif(actif).build();
    }

    private Utilisateur gardienActif() {
        return Utilisateur.builder().id(GARDIEN_ID).nom("Diop").prenom("Amy").role(Role.GARDIEN).actif(true).build();
    }

    private ResidenceGardienRequestDto dto() {
        return new ResidenceGardienRequestDto(RESIDENCE_ID, GARDIEN_ID, false);
    }

    @Test
    void affecterEchoueSiLaResidenceEstDesactivee() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(false)));

        assertThatThrownBy(() -> residenceGardienService.affecter(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidence est désactivée");
    }

    @Test
    void affecterFonctionnePourUneResidenceActive() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(true)));
        when(utilisateurRepository.findById(GARDIEN_ID)).thenReturn(Optional.of(gardienActif()));
        when(residenceGardienRepository.save(any(ResidenceGardien.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidenceGardienResponseDto result = residenceGardienService.affecter(dto());

        assertThat(result.residenceId()).isEqualTo(RESIDENCE_ID);
        assertThat(result.gardienId()).isEqualTo(GARDIEN_ID);
    }
}
