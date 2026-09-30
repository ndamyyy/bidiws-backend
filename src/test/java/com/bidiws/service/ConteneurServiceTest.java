package com.bidiws.service;

import com.bidiws.dto.conteneur.ConteneurRequestDto;
import com.bidiws.dto.conteneur.ConteneurResponseDto;
import com.bidiws.entity.Conteneur;
import com.bidiws.entity.Residence;
import com.bidiws.repository.ArretConteneurRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.repository.ResidenceRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConteneurServiceTest {

    @Mock
    private ConteneurRepository conteneurRepository;
    @Mock
    private ResidenceRepository residenceRepository;
    @Mock
    private ArretConteneurRepository arretConteneurRepository;

    @InjectMocks
    private ConteneurService conteneurService;

    private static final Long RESIDENCE_ID = 1L;
    private static final Long CONTENEUR_ID = 2L;

    private Residence residence() {
        return Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").actif(true).build();
    }

    private Conteneur conteneur() {
        return Conteneur.builder().id(CONTENEUR_ID).code("023").residence(residence()).rfidTag("TAG-023").actif(true).build();
    }

    private ConteneurRequestDto dto(String code, String rfidTag) {
        return new ConteneurRequestDto(code, RESIDENCE_ID, rfidTag);
    }

    @Test
    void createEchoueSiLaResidenceEstIntrouvable() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conteneurService.create(dto("023", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Résidence introuvable");
    }

    @Test
    void createEchoueSiLaResidenceEstDesactivee() {
        Residence residenceInactive = Residence.builder().id(RESIDENCE_ID).nom("Résidence Test").actif(false).build();
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residenceInactive));

        assertThatThrownBy(() -> conteneurService.create(dto("023", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("résidence est désactivée");
    }

    @Test
    void createEchoueSiLeCodeExisteDejaPourLaResidence() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence()));
        when(conteneurRepository.existsByResidenceIdAndCode(RESIDENCE_ID, "023")).thenReturn(true);

        assertThatThrownBy(() -> conteneurService.create(dto("023", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void createSauvegardeLeConteneur() {
        when(residenceRepository.findById(RESIDENCE_ID)).thenReturn(Optional.of(residence()));
        when(conteneurRepository.existsByResidenceIdAndCode(RESIDENCE_ID, "023")).thenReturn(false);
        when(conteneurRepository.save(any(Conteneur.class))).thenAnswer(inv -> {
            Conteneur c = inv.getArgument(0);
            c.setId(CONTENEUR_ID);
            return c;
        });

        ConteneurResponseDto result = conteneurService.create(dto("023", "TAG-023"));

        assertThat(result.code()).isEqualTo("023");
        assertThat(result.rfidTag()).isEqualTo("TAG-023");
        assertThat(result.residenceId()).isEqualTo(RESIDENCE_ID);
    }

    @Test
    void updateEchoueSiLaResidenceChange() {
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(conteneur()));

        ConteneurRequestDto dtoAutreResidence = new ConteneurRequestDto("023", 99L, "TAG-023");

        assertThatThrownBy(() -> conteneurService.update(CONTENEUR_ID, dtoAutreResidence))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("changer de résidence");
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conteneurService.getById(CONTENEUR_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conteneur introuvable");
    }

    @Test
    void getByResidenceRetourneLaListe() {
        when(conteneurRepository.findByResidenceId(RESIDENCE_ID)).thenReturn(List.of(conteneur()));

        assertThat(conteneurService.getByResidence(RESIDENCE_ID)).hasSize(1);
    }

    @Test
    void desactiverMetActifAFalse() {
        Conteneur c = conteneur();
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(c));
        when(arretConteneurRepository.existsActifByConteneurId(CONTENEUR_ID)).thenReturn(false);

        conteneurService.desactiver(CONTENEUR_ID);

        assertThat(c.getActif()).isFalse();
    }

    @Test
    void desactiverEchoueSiUneLigneArretConteneurEstEnAttenteSurUneTourneeActive() {
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(conteneur()));
        when(arretConteneurRepository.existsActifByConteneurId(CONTENEUR_ID)).thenReturn(true);

        assertThatThrownBy(() -> conteneurService.desactiver(CONTENEUR_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("arrêt en attente sur une tournée active");
    }

    @Test
    void desactiverFonctionneSansArretEnAttenteSurTourneeActive() {
        Conteneur c = conteneur();
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(c));
        when(arretConteneurRepository.existsActifByConteneurId(CONTENEUR_ID)).thenReturn(false);

        assertThatCode(() -> conteneurService.desactiver(CONTENEUR_ID)).doesNotThrowAnyException();
        assertThat(c.getActif()).isFalse();
    }
}
