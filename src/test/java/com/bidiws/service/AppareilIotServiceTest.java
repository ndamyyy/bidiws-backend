package com.bidiws.service;

import com.bidiws.dto.appareiliot.AppareilIotCreeResponseDto;
import com.bidiws.dto.appareiliot.AppareilIotRequestDto;
import com.bidiws.entity.AppareilIot;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Conteneur;
import com.bidiws.enums.TypeAppareilIot;
import com.bidiws.repository.AppareilIotRepository;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.security.ApiKeyHasher;
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
class AppareilIotServiceTest {

    @Mock
    private AppareilIotRepository appareilIotRepository;
    @Mock
    private ConteneurRepository conteneurRepository;
    @Mock
    private CamionRepository camionRepository;
    @Mock
    private ApiKeyHasher apiKeyHasher;

    @InjectMocks
    private AppareilIotService appareilIotService;

    private static final Long APPAREIL_ID = 1L;
    private static final Long CONTENEUR_ID = 2L;
    private static final Long CAMION_ID = 3L;

    private Conteneur conteneur() {
        return Conteneur.builder().id(CONTENEUR_ID).code("023").build();
    }

    private Camion camion() {
        return Camion.builder().id(CAMION_ID).immatriculation("AB-123-CD").build();
    }

    @Test
    void createEchoueSiRattacheAConteneurEtCamionALaFois() {
        AppareilIotRequestDto dto = new AppareilIotRequestDto("AA:BB:CC", TypeAppareilIot.CAPTEUR_BENNE, CONTENEUR_ID, CAMION_ID);
        when(appareilIotRepository.existsByIdentifiantMateriel("AA:BB:CC")).thenReturn(false);

        assertThatThrownBy(() -> appareilIotService.create(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("à la fois");
    }

    @Test
    void createEchoueSiLidentifiantMaterielExisteDeja() {
        AppareilIotRequestDto dto = new AppareilIotRequestDto("AA:BB:CC", TypeAppareilIot.CAPTEUR_BENNE, CONTENEUR_ID, null);
        when(appareilIotRepository.existsByIdentifiantMateriel("AA:BB:CC")).thenReturn(true);

        assertThatThrownBy(() -> appareilIotService.create(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà enregistré");
    }

    @Test
    void createGenereEtHashLaCleApiEtLaRetourneEnClairUneSeuleFois() {
        AppareilIotRequestDto dto = new AppareilIotRequestDto("AA:BB:CC", TypeAppareilIot.CAPTEUR_BENNE, CONTENEUR_ID, null);
        when(appareilIotRepository.existsByIdentifiantMateriel("AA:BB:CC")).thenReturn(false);
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.of(conteneur()));
        when(apiKeyHasher.genererCle()).thenReturn("cle-en-clair");
        when(apiKeyHasher.hash("cle-en-clair")).thenReturn("hash-simule");
        when(appareilIotRepository.save(any(AppareilIot.class))).thenAnswer(inv -> {
            AppareilIot a = inv.getArgument(0);
            a.setId(APPAREIL_ID);
            return a;
        });

        AppareilIotCreeResponseDto result = appareilIotService.create(dto);

        assertThat(result.cleApi()).isEqualTo("cle-en-clair");
        assertThat(result.identifiantMateriel()).isEqualTo("AA:BB:CC");
    }

    @Test
    void createEchoueSiLeConteneurEstIntrouvable() {
        AppareilIotRequestDto dto = new AppareilIotRequestDto("AA:BB:CC", TypeAppareilIot.CAPTEUR_BENNE, CONTENEUR_ID, null);
        when(appareilIotRepository.existsByIdentifiantMateriel("AA:BB:CC")).thenReturn(false);
        when(conteneurRepository.findById(CONTENEUR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appareilIotService.create(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conteneur introuvable");
    }

    @Test
    void createRattacheAuCamionQuandCamionIdFourni() {
        AppareilIotRequestDto dto = new AppareilIotRequestDto("READER-1", TypeAppareilIot.LECTEUR_RFID, null, CAMION_ID);
        when(appareilIotRepository.existsByIdentifiantMateriel("READER-1")).thenReturn(false);
        when(camionRepository.findById(CAMION_ID)).thenReturn(Optional.of(camion()));
        when(apiKeyHasher.genererCle()).thenReturn("cle-en-clair");
        when(apiKeyHasher.hash("cle-en-clair")).thenReturn("hash-simule");
        when(appareilIotRepository.save(any(AppareilIot.class))).thenAnswer(inv -> inv.getArgument(0));

        AppareilIotCreeResponseDto result = appareilIotService.create(dto);

        assertThat(result.identifiantMateriel()).isEqualTo("READER-1");
    }

    @Test
    void getByIdEchoueSiIntrouvable() {
        when(appareilIotRepository.findById(APPAREIL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appareilIotService.getById(APPAREIL_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Appareil introuvable");
    }

    @Test
    void desactiverMetActifAFalse() {
        AppareilIot a = AppareilIot.builder().id(APPAREIL_ID).identifiantMateriel("AA:BB:CC").actif(true).build();
        when(appareilIotRepository.findById(APPAREIL_ID)).thenReturn(Optional.of(a));

        appareilIotService.desactiver(APPAREIL_ID);

        assertThat(a.getActif()).isFalse();
    }

    @Test
    void regenererCleChangeLeHashEtRetourneLaNouvelleCleEnClair() {
        AppareilIot a = AppareilIot.builder().id(APPAREIL_ID).identifiantMateriel("AA:BB:CC").cleApiHash("ancien-hash").build();
        when(appareilIotRepository.findById(APPAREIL_ID)).thenReturn(Optional.of(a));
        when(apiKeyHasher.genererCle()).thenReturn("nouvelle-cle");
        when(apiKeyHasher.hash("nouvelle-cle")).thenReturn("nouveau-hash");
        when(appareilIotRepository.save(any(AppareilIot.class))).thenAnswer(inv -> inv.getArgument(0));

        AppareilIotCreeResponseDto result = appareilIotService.regenererCle(APPAREIL_ID);

        assertThat(result.cleApi()).isEqualTo("nouvelle-cle");
        assertThat(a.getCleApiHash()).isEqualTo("nouveau-hash");
    }
}
