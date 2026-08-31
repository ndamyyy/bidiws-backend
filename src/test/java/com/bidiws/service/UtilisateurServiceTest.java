package com.bidiws.service;

import com.bidiws.dto.utilisateur.ChangePasswordRequestDto;
import com.bidiws.dto.utilisateur.ResetPasswordRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurAdminCreateRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurRegisterRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurResponseDto;
import com.bidiws.dto.utilisateur.UtilisateurUpdateRequestDto;
import com.bidiws.entity.ChauffeurCamion;
import com.bidiws.entity.Utilisateur;
import com.bidiws.entity.Ville;
import com.bidiws.enums.Role;
import com.bidiws.enums.StatutTournee;
import com.bidiws.repository.ChauffeurCamionRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.repository.VilleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private VilleRepository villeRepository;
    @Mock
    private ChauffeurCamionRepository chauffeurCamionRepository;
    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UtilisateurService utilisateurService;

    private static final Long UTILISATEUR_ID = 1L;
    private static final Long VILLE_ID = 2L;
    private static final String EMAIL = "user@bidiws.com";

    private Utilisateur utilisateur(Role role) {
        return Utilisateur.builder()
                .id(UTILISATEUR_ID)
                .email(EMAIL)
                .motDePasse("hash-actuel")
                .nom("Diop")
                .prenom("Amy")
                .role(role)
                .actif(true)
                .build();
    }

    @Test
    void registerEchoueSiLemailExisteDeja() {
        UtilisateurRegisterRequestDto dto = new UtilisateurRegisterRequestDto(EMAIL, "motdepasse123", "Diop", "Amy", null);
        when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.register(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Un compte existe déjà");
    }

    @Test
    void registerCreeUnCompteHabitantAvecMotDePasseHashe() {
        UtilisateurRegisterRequestDto dto = new UtilisateurRegisterRequestDto(EMAIL, "motdepasse123", "Diop", "Amy", "770000000");
        when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(UTILISATEUR_ID);
            return u;
        });

        UtilisateurResponseDto result = utilisateurService.register(dto);

        assertThat(result.role()).isEqualTo(Role.HABITANT);
        assertThat(result.email()).isEqualTo(EMAIL);
    }

    @Test
    void updateEchoueSiLeNouvelEmailEstDejaUtilisePartUnAutreCompte() {
        UtilisateurUpdateRequestDto dto = new UtilisateurUpdateRequestDto("Diop", "Amy", "autre@bidiws.com", null);
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.HABITANT)));
        when(utilisateurRepository.existsByEmail("autre@bidiws.com")).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.update(UTILISATEUR_ID, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà utilisé");
    }

    @Test
    void updateNeVerifiePasLunicitePourLePropreEmailInchange() {
        UtilisateurUpdateRequestDto dto = new UtilisateurUpdateRequestDto("Diop", "Amy", EMAIL, "770000000");
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.HABITANT)));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurResponseDto result = utilisateurService.update(UTILISATEUR_ID, dto);

        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.telephone()).isEqualTo("770000000");
    }

    @Test
    void createByAdminEchoueSiLemailExisteDeja() {
        UtilisateurAdminCreateRequestDto dto = new UtilisateurAdminCreateRequestDto(EMAIL, "motdepasse123", "Diop", "Amy", null, Role.GARDIEN);
        when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.createByAdmin(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Un compte existe déjà");
    }

    @Test
    void createByAdminCreeUnCompteAvecLeRoleDemande() {
        UtilisateurAdminCreateRequestDto dto = new UtilisateurAdminCreateRequestDto(EMAIL, "motdepasse123", "Diop", "Amy", null, Role.GARDIEN);
        when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurResponseDto result = utilisateurService.createByAdmin(dto);

        assertThat(result.role()).isEqualTo(Role.GARDIEN);
    }

    @Test
    void rechercherParNomOuPrenom() {
        when(utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase("Diop", ""))
                .thenReturn(List.of(utilisateur(Role.HABITANT)));

        assertThat(utilisateurService.rechercher("Diop", null, null)).hasSize(1);
    }

    @Test
    void rechercherParRoleQuandAucunNomPrenom() {
        when(utilisateurRepository.findByRole(Role.GARDIEN)).thenReturn(List.of(utilisateur(Role.GARDIEN)));

        assertThat(utilisateurService.rechercher(null, null, Role.GARDIEN)).hasSize(1);
    }

    @Test
    void rechercherRetourneToutSansFiltre() {
        when(utilisateurRepository.findAll()).thenReturn(List.of(utilisateur(Role.HABITANT), utilisateur(Role.ADMIN)));

        assertThat(utilisateurService.rechercher(null, null, null)).hasSize(2);
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.getById(UTILISATEUR_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    // Sert de base a GET /utilisateurs/moi (UtilisateurController.moi), qui
    // delegue directement a getById(userDetails.getId()) sans logique
    // supplementaire — reutilise le mapping existant, rien a dupliquer.
    @Test
    void getByIdRetourneLesInfosDeLUtilisateur() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.CHAUFFEUR)));

        UtilisateurResponseDto result = utilisateurService.getById(UTILISATEUR_ID);

        assertThat(result.id()).isEqualTo(UTILISATEUR_ID);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.role()).isEqualTo(Role.CHAUFFEUR);
    }

    @Test
    void setActifDesactiveLeCompte() {
        Utilisateur u = utilisateur(Role.HABITANT);
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(u));

        utilisateurService.setActif(UTILISATEUR_ID, false);

        assertThat(u.getActif()).isFalse();
    }

    @Test
    void setActifEchoueSiLeChauffeurAUneAffectationActive() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.CHAUFFEUR)));
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(UTILISATEUR_ID))
                .thenReturn(Optional.of(ChauffeurCamion.builder().build()));

        assertThatThrownBy(() -> utilisateurService.setActif(UTILISATEUR_ID, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("affectation active ou une tournée en cours");
    }

    @Test
    void setActifEchoueSiLeChauffeurAUneTourneeEnCours() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.CHAUFFEUR)));
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(UTILISATEUR_ID)).thenReturn(Optional.empty());
        when(tourneeRepository.existsByChauffeurIdAndStatut(UTILISATEUR_ID, StatutTournee.EN_COURS)).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.setActif(UTILISATEUR_ID, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("affectation active ou une tournée en cours");
    }

    @Test
    void setActifDesactiveUnChauffeurSansAffectationNiTourneeEnCours() {
        Utilisateur u = utilisateur(Role.CHAUFFEUR);
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(u));
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(UTILISATEUR_ID)).thenReturn(Optional.empty());
        when(tourneeRepository.existsByChauffeurIdAndStatut(UTILISATEUR_ID, StatutTournee.EN_COURS)).thenReturn(false);

        assertThatCode(() -> utilisateurService.setActif(UTILISATEUR_ID, false)).doesNotThrowAnyException();

        assertThat(u.getActif()).isFalse();
    }

    @Test
    void changerRoleMetAJourLeRole() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.HABITANT)));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurResponseDto result = utilisateurService.changerRole(UTILISATEUR_ID, Role.GARDIEN);

        assertThat(result.role()).isEqualTo(Role.GARDIEN);
    }

    @Test
    void changerRoleEchoueSiLeChauffeurAUneAffectationActive() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.CHAUFFEUR)));
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(UTILISATEUR_ID))
                .thenReturn(Optional.of(ChauffeurCamion.builder().build()));

        assertThatThrownBy(() -> utilisateurService.changerRole(UTILISATEUR_ID, Role.GARDIEN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("affectation active ou une tournée en cours");
    }

    @Test
    void changerRoleEchoueSiLeChauffeurAUneTourneeEnCours() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.CHAUFFEUR)));
        when(chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(UTILISATEUR_ID)).thenReturn(Optional.empty());
        when(tourneeRepository.existsByChauffeurIdAndStatut(UTILISATEUR_ID, StatutTournee.EN_COURS)).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.changerRole(UTILISATEUR_ID, Role.GARDIEN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("affectation active ou une tournée en cours");
    }

    @Test
    void changerRoleVersChauffeurNestJamaisBloquePeuImporteLetat() {
        // Promotion vers CHAUFFEUR : aucune verification d'affectation/tournee
        // n'a de sens ici, meme si les repos ne sont pas stubbes (jamais appeles).
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.HABITANT)));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurResponseDto result = utilisateurService.changerRole(UTILISATEUR_ID, Role.CHAUFFEUR);

        assertThat(result.role()).isEqualTo(Role.CHAUFFEUR);
    }

    @Test
    void changerVilleEchoueSiLeCompteNestPasMairie() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.HABITANT)));

        assertThatThrownBy(() -> utilisateurService.changerVille(UTILISATEUR_ID, VILLE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("possible que pour un compte MAIRIE");
    }

    @Test
    void changerVilleRattacheLaMairieALaVille() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.MAIRIE)));
        when(villeRepository.findById(VILLE_ID)).thenReturn(Optional.of(Ville.builder().id(VILLE_ID).nom("Dakar").build()));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurResponseDto result = utilisateurService.changerVille(UTILISATEUR_ID, VILLE_ID);

        assertThat(result.villeId()).isEqualTo(VILLE_ID);
    }

    @Test
    void changerVilleAvecIdNullRetireLeRattachement() {
        Utilisateur mairie = utilisateur(Role.MAIRIE);
        mairie.setVille(Ville.builder().id(VILLE_ID).nom("Dakar").build());
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(mairie));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurResponseDto result = utilisateurService.changerVille(UTILISATEUR_ID, null);

        assertThat(result.villeId()).isNull();
    }

    @Test
    void resetMotDePasseByAdminEncodeLeNouveauMotDePasse() {
        Utilisateur u = utilisateur(Role.HABITANT);
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("nouveauMdp123")).thenReturn("hash-nouveau");

        utilisateurService.resetMotDePasseByAdmin(UTILISATEUR_ID, new ResetPasswordRequestDto("nouveauMdp123"));

        assertThat(u.getMotDePasse()).isEqualTo("hash-nouveau");
    }

    @Test
    void changerMotDePasseEchoueSiLancienMotDePasseEstIncorrect() {
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(utilisateur(Role.HABITANT)));
        when(passwordEncoder.matches("mauvais", "hash-actuel")).thenReturn(false);

        assertThatThrownBy(() -> utilisateurService.changerMotDePasse(
                UTILISATEUR_ID, new ChangePasswordRequestDto("mauvais", "nouveauMdp123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ancien mot de passe incorrect");
    }

    @Test
    void changerMotDePasseFonctionneAvecLeBonAncienMotDePasse() {
        Utilisateur u = utilisateur(Role.HABITANT);
        when(utilisateurRepository.findById(UTILISATEUR_ID)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("ancien123", "hash-actuel")).thenReturn(true);
        when(passwordEncoder.encode("nouveauMdp123")).thenReturn("hash-nouveau");

        utilisateurService.changerMotDePasse(UTILISATEUR_ID, new ChangePasswordRequestDto("ancien123", "nouveauMdp123"));

        assertThat(u.getMotDePasse()).isEqualTo("hash-nouveau");
    }
}
