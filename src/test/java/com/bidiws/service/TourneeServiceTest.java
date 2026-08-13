package com.bidiws.service;

import com.bidiws.dto.tournee.TourneeRequestDto;
import com.bidiws.dto.tournee.TourneeResponseDto;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Tournee;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.entity.Utilisateur;
import com.bidiws.entity.Zone;
import com.bidiws.enums.Role;
import com.bidiws.enums.StatutTournee;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.TypeCollecteRepository;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.repository.ZoneRepository;
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
class TourneeServiceTest {

    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private TypeCollecteRepository typeCollecteRepository;
    @Mock
    private CamionRepository camionRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private TourneeService tourneeService;

    private static final Long TOURNEE_ID = 1L;
    private static final Long TYPE_COLLECTE_ID = 2L;
    private static final Long CAMION_ID = 3L;
    private static final Long CHAUFFEUR_ID = 4L;

    private TypeCollecte typeCollecte() {
        return TypeCollecte.builder().id(TYPE_COLLECTE_ID).code("OM").libelle("Ordures ménagères").build();
    }

    private Camion camionActif() {
        return Camion.builder().id(CAMION_ID).immatriculation("AB-123-CD").actif(true).build();
    }

    private Utilisateur chauffeurActif() {
        return Utilisateur.builder().id(CHAUFFEUR_ID).nom("Diop").prenom("Amy").role(Role.CHAUFFEUR).actif(true).build();
    }

    private Tournee tournee(StatutTournee statut) {
        return Tournee.builder()
                .id(TOURNEE_ID)
                .dateTournee(LocalDate.of(2026, 8, 1))
                .typeCollecte(typeCollecte())
                .camion(camionActif())
                .chauffeur(chauffeurActif())
                .statut(statut)
                .build();
    }

    private TourneeRequestDto dto() {
        return new TourneeRequestDto(LocalDate.of(2026, 8, 1), TYPE_COLLECTE_ID, CAMION_ID, CHAUFFEUR_ID, null);
    }

    @Test
    void createEchoueSiLeChauffeurNestPasUnChauffeurActif() {
        Utilisateur gardien = Utilisateur.builder().id(CHAUFFEUR_ID).role(Role.GARDIEN).actif(true).build();
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionActif()));
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(gardien));

        assertThatThrownBy(() -> tourneeService.create(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pas un chauffeur actif");
    }

    @Test
    void createEchoueSiLeCamionEstInactif() {
        Camion camionInactif = Camion.builder().id(CAMION_ID).immatriculation("AB-123-CD").actif(false).build();
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionInactif));
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(chauffeurActif()));

        assertThatThrownBy(() -> tourneeService.create(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("camion sélectionné est inactif");
    }

    @Test
    void createSauvegardeLaTourneeAvecStatutPlanifiee() {
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionActif()));
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(chauffeurActif()));
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> {
            Tournee t = inv.getArgument(0);
            t.setId(TOURNEE_ID);
            return t;
        });

        TourneeResponseDto result = tourneeService.create(dto());

        assertThat(result.statut()).isEqualTo(StatutTournee.PLANIFIEE);
        assertThat(result.chauffeurId()).isEqualTo(CHAUFFEUR_ID);
    }

    @Test
    void createEchoueSiLaZoneEstIntrouvable() {
        TourneeRequestDto dtoAvecZone = new TourneeRequestDto(LocalDate.of(2026, 8, 1), TYPE_COLLECTE_ID, CAMION_ID, CHAUFFEUR_ID, 99L);
        when(typeCollecteRepository.findById(TYPE_COLLECTE_ID)).thenReturn(Optional.of(typeCollecte()));
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camionActif()));
        when(utilisateurRepository.findById(CHAUFFEUR_ID)).thenReturn(Optional.of(chauffeurActif()));
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourneeService.create(dtoAvecZone))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Zone introuvable");
    }

    @Test
    void demarrerFaitPasserDePlanifieeAEnCours() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.PLANIFIEE)));
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> inv.getArgument(0));

        TourneeResponseDto result = tourneeService.demarrer(TOURNEE_ID);

        assertThat(result.statut()).isEqualTo(StatutTournee.EN_COURS);
    }

    @Test
    void demarrerEchoueSiPasPlanifiee() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.EN_COURS)));

        assertThatThrownBy(() -> tourneeService.demarrer(TOURNEE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("planifiée");
    }

    @Test
    void terminerFaitPasserDEnCoursATerminee() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.EN_COURS)));
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> inv.getArgument(0));

        TourneeResponseDto result = tourneeService.terminer(TOURNEE_ID);

        assertThat(result.statut()).isEqualTo(StatutTournee.TERMINEE);
    }

    @Test
    void terminerEchoueSiPasEnCours() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.PLANIFIEE)));

        assertThatThrownBy(() -> tourneeService.terminer(TOURNEE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("en cours");
    }

    @Test
    void annulerEchoueSiDejaTerminee() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.TERMINEE)));

        assertThatThrownBy(() -> tourneeService.annuler(TOURNEE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà terminée");
    }

    @Test
    void annulerFonctionnePourUneTourneePlanifiee() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee(StatutTournee.PLANIFIEE)));
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> inv.getArgument(0));

        TourneeResponseDto result = tourneeService.annuler(TOURNEE_ID);

        assertThat(result.statut()).isEqualTo(StatutTournee.ANNULEE);
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourneeService.getById(TOURNEE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tournée introuvable");
    }

    @Test
    void getByDateRetourneLaListe() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(tourneeRepository.findByDateTournee(date)).thenReturn(List.of(tournee(StatutTournee.PLANIFIEE)));

        assertThat(tourneeService.getByDate(date)).hasSize(1);
    }

    @Test
    void getByChauffeurRetourneLaListe() {
        when(tourneeRepository.findByChauffeurId(CHAUFFEUR_ID)).thenReturn(List.of(tournee(StatutTournee.PLANIFIEE)));

        assertThat(tourneeService.getByChauffeur(CHAUFFEUR_ID)).hasSize(1);
    }

    @Test
    void getAllRetourneToutesLesTournees() {
        when(tourneeRepository.findAll()).thenReturn(List.of(tournee(StatutTournee.PLANIFIEE), tournee(StatutTournee.TERMINEE)));

        assertThat(tourneeService.getAll()).hasSize(2);
    }
}
