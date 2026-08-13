package com.bidiws.service;

import com.bidiws.dto.chauffeurcamion.ChauffeurCamionRequestDto;
import com.bidiws.dto.chauffeurcamion.ChauffeurCamionResponseDto;
import com.bidiws.entity.Camion;
import com.bidiws.entity.ChauffeurCamion;
import com.bidiws.entity.ChauffeurCamionId;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ChauffeurCamionRepository;
import com.bidiws.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChauffeurCamionServiceTest {

    @Mock
    private ChauffeurCamionRepository chauffeurCamionRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private CamionRepository camionRepository;

    @InjectMocks
    private ChauffeurCamionService chauffeurCamionService;

    private static final Long CHAUFFEUR_ID = 1L;
    private static final Long CAMION_ID = 2L;
    private static final Long AUTRE_CHAUFFEUR_ID = 3L;

    private Utilisateur chauffeurActif(Long id) {
        return Utilisateur.builder().id(id).nom("Diop").prenom("Amy").role(Role.CHAUFFEUR).actif(true).build();
    }

    private Camion camion() {
        return Camion.builder().id(CAMION_ID).immatriculation("AB-123-CD").build();
    }

    private ChauffeurCamionRequestDto dto() {
        return new ChauffeurCamionRequestDto(CHAUFFEUR_ID, CAMION_ID, LocalDate.of(2026, 8, 1), null);
    }

    private ChauffeurCamion affectationActive(Long chauffeurId) {
        return ChauffeurCamion.builder()
                .chauffeurCamionId(new ChauffeurCamionId(chauffeurId, CAMION_ID, LocalDate.of(2026, 8, 1)))
                .chauffeur(chauffeurActif(chauffeurId))
                .camion(camion())
                .dateFin(null)
                .build();
    }

    @Test
    void affecterEchoueSiLutilisateurNestPasUnChauffeurActif() {
        Utilisateur gardien = Utilisateur.builder().id(CHAUFFEUR_ID).role(Role.GARDIEN).actif(true).build();
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(gardien));

        assertThatThrownBy(() -> chauffeurCamionService.affecter(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pas un chauffeur actif");
    }

    @Test
    void affecterEchoueSiLeCamionADejaUnChauffeur() {
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(chauffeurActif(CHAUFFEUR_ID)));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camion()));
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID))
                .thenReturn(Optional.of(affectationActive(AUTRE_CHAUFFEUR_ID)));

        assertThatThrownBy(() -> chauffeurCamionService.affecter(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà un chauffeur");
    }

    @Test
    void affecterEchoueSiLeChauffeurConduitDejaUnAutreCamion() {
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(chauffeurActif(CHAUFFEUR_ID)));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camion()));
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID)).thenReturn(Optional.empty());
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(CHAUFFEUR_ID))
                .thenReturn(Optional.of(affectationActive(CHAUFFEUR_ID)));

        assertThatThrownBy(() -> chauffeurCamionService.affecter(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("conduit déjà un autre camion");
    }

    @Test
    void affecterCreeLaffectationQuandTousLesControlesPassent() {
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(chauffeurActif(CHAUFFEUR_ID)));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camion()));
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID)).thenReturn(Optional.empty());
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(CHAUFFEUR_ID)).thenReturn(Optional.empty());
        when(chauffeurCamionRepository.save(any(ChauffeurCamion.class))).thenAnswer(inv -> inv.getArgument(0));

        ChauffeurCamionResponseDto result = chauffeurCamionService.affecter(dto());

        assertThat(result.chauffeurId()).isEqualTo(CHAUFFEUR_ID);
        assertThat(result.camionId()).isEqualTo(CAMION_ID);
    }

    @Test
    void terminerEchoueSiAucuneAffectationActive() {
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chauffeurCamionService.terminer(CHAUFFEUR_ID, CAMION_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucune affectation active");
    }

    @Test
    void terminerEchoueSiLeChauffeurNestPasCeluiAffecte() {
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID))
                .thenReturn(Optional.of(affectationActive(AUTRE_CHAUFFEUR_ID)));

        assertThatThrownBy(() -> chauffeurCamionService.terminer(CHAUFFEUR_ID, CAMION_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'est pas celui affecté");
    }

    @Test
    void terminerClotureLaffectationPourLeBonChauffeur() {
        when(chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(CAMION_ID))
                .thenReturn(Optional.of(affectationActive(CHAUFFEUR_ID)));
        when(chauffeurCamionRepository.save(any(ChauffeurCamion.class))).thenAnswer(inv -> inv.getArgument(0));

        ChauffeurCamionResponseDto result = chauffeurCamionService.terminer(CHAUFFEUR_ID, CAMION_ID);

        assertThat(result.dateFin()).isEqualTo(LocalDate.now());
    }

    @Test
    void getByChauffeurRetourneLaListe() {
        when(chauffeurCamionRepository.findByChauffeurId(CHAUFFEUR_ID)).thenReturn(List.of(affectationActive(CHAUFFEUR_ID)));

        assertThat(chauffeurCamionService.getByChauffeur(CHAUFFEUR_ID)).hasSize(1);
    }

    @Test
    void getByCamionRetourneLaListe() {
        when(chauffeurCamionRepository.findByCamionId(CAMION_ID)).thenReturn(List.of(affectationActive(CHAUFFEUR_ID)));

        assertThat(chauffeurCamionService.getByCamion(CAMION_ID)).hasSize(1);
    }
}
