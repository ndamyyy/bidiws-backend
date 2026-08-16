package com.bidiws.service;

import com.bidiws.dto.residencesyndic.ResidenceSyndicRequestDto;
import com.bidiws.dto.residencesyndic.ResidenceSyndicResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.ResidenceSyndic;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.ResidenceSyndicRepository;
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
class ResidenceSyndicServiceTest {

    @Mock
    private ResidenceSyndicRepository residenceSyndicRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ResidenceSyndicService residenceSyndicService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long SYNDIC_ID = 2L;

    private Residence residence(boolean actif) {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").actif(actif).build();
    }

    private Utilisateur syndicActif() {
        return Utilisateur.builder().id(SYNDIC_ID).nom("Diop").prenom("Amy").role(Role.SYNDIC).actif(true).build();
    }

    private ResidenceSyndicRequestDto dto() {
        return new ResidenceSyndicRequestDto(RESIDENCE_ID, SYNDIC_ID);
    }

    @Test
    void affecterEchoueSiLaResidenceEstDesactivee() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(false)));

        assertThatThrownBy(() -> residenceSyndicService.affecter(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidence est désactivée");
    }

    @Test
    void affecterFonctionnePourUneResidenceActive() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence(true)));
        when(utilisateurRepository.findById(SYNDIC_ID)).thenReturn(Optional.of(syndicActif()));
        when(residenceSyndicRepository.save(any(ResidenceSyndic.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidenceSyndicResponseDto result = residenceSyndicService.affecter(dto());

        assertThat(result.residenceId()).isEqualTo(RESIDENCE_ID);
        assertThat(result.syndicId()).isEqualTo(SYNDIC_ID);
    }
}
