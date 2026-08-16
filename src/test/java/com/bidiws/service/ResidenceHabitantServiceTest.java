package com.bidiws.service;

import com.bidiws.dto.residencehabitant.ResidenceHabitantRequestDto;
import com.bidiws.dto.residencehabitant.ResidenceHabitantResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.ResidenceHabitant;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.ResidenceHabitantRepository;
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
class ResidenceHabitantServiceTest {

    @Mock
    private ResidenceHabitantRepository residenceHabitantRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ResidenceHabitantService residenceHabitantService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long HABITANT_ID = 2L;

    private Residence residence(boolean actif) {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").actif(actif).build();
    }

    private Utilisateur habitantActif() {
        return Utilisateur.builder().id(HABITANT_ID).nom("Diop").prenom("Amy").role(Role.HABITANT).actif(true).build();
    }

    private ResidenceHabitantRequestDto dto() {
        return new ResidenceHabitantRequestDto(RESIDENCE_ID, HABITANT_ID);
    }

    @Test
    void affecterEchoueSiLaResidenceEstDesactivee() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(false)));

        assertThatThrownBy(() -> residenceHabitantService.affecter(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidence est désactivée");
    }

    @Test
    void affecterFonctionnePourUneResidenceActive() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(true)));
        when(utilisateurRepository.findById(HABITANT_ID)).thenReturn(Optional.of(habitantActif()));
        when(residenceHabitantRepository.save(any(ResidenceHabitant.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidenceHabitantResponseDto result = residenceHabitantService.affecter(dto());

        assertThat(result.residenceId()).isEqualTo(RESIDENCE_ID);
        assertThat(result.habitantId()).isEqualTo(HABITANT_ID);
    }
}
