package com.bidiws.service;

import com.bidiws.dto.appareiliot.AppareilIotCreeResponseDto;
import com.bidiws.dto.appareiliot.AppareilIotImportLigneErreurDto;
import com.bidiws.dto.appareiliot.AppareilIotImportResultatDto;
import com.bidiws.dto.appareiliot.AppareilIotRequestDto;
import com.bidiws.dto.appareiliot.AppareilIotResponseDto;
import com.bidiws.entity.AppareilIot;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Conteneur;
import com.bidiws.enums.TypeAppareilIot;
import com.bidiws.repository.AppareilIotRepository;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.security.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppareilIotService {

    private final AppareilIotRepository appareilIotRepository;
    private final ConteneurRepository conteneurRepository;
    private final CamionRepository camionRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Transactional
    public AppareilIotCreeResponseDto create(AppareilIotRequestDto dto) {

        if (appareilIotRepository.existsByIdentifiantMateriel(dto.identifiantMateriel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un appareil avec cet identifiant matériel est déjà enregistré");
        }

        AppareilIot appareil = AppareilIot.builder()
                .identifiantMateriel(dto.identifiantMateriel())
                .typeAppareil(dto.typeAppareil())
                .actif(true)
                .build();

        appliquerRattachement(appareil, dto.conteneurId(), dto.camionId());

        String cleEnClair = apiKeyHasher.genererCle();
        appareil.setCleApiHash(apiKeyHasher.hash(cleEnClair));

        AppareilIot saved = appareilIotRepository.save(appareil);

        return new AppareilIotCreeResponseDto(saved.getId(), saved.getIdentifiantMateriel(), cleEnClair);
    }

    @Transactional
    public AppareilIotResponseDto update(Long id, AppareilIotRequestDto dto) {

        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));

        if (!appareil.getIdentifiantMateriel().equals(dto.identifiantMateriel())
                && appareilIotRepository.existsByIdentifiantMateriel(dto.identifiantMateriel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un appareil avec cet identifiant matériel est déjà enregistré");
        }

        appareil.setIdentifiantMateriel(dto.identifiantMateriel());
        appareil.setTypeAppareil(dto.typeAppareil());
        appliquerRattachement(appareil, dto.conteneurId(), dto.camionId());

        return toResponseDto(appareilIotRepository.save(appareil));
    }

    // Un appareil doit etre rattache a exactement un des deux (conteneur XOR
    // camion). Le cas "les deux" est aussi bloque par la contrainte
    // chk_appareil_iot_un_seul_rattachement en base, mais celle-ci n'interdit
    // pas "aucun des deux" (les deux colonnes sont nullable cote schema) : sans
    // ce check applicatif, un appareil sans aucun rattachement etait cree
    // silencieusement, sans erreur DB ni 400.
    private void appliquerRattachement(AppareilIot appareil, Long conteneurId, Long camionId) {

        if ((conteneurId == null) == (camionId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un appareil doit être rattaché à un conteneur ou un camion, pas les deux ni aucun des deux");
        }

        if (conteneurId != null) {
            Conteneur conteneur = conteneurRepository.findById(conteneurId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteneur introuvable"));
            if (!Boolean.TRUE.equals(conteneur.getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Impossible de rattacher un appareil à un conteneur inactif");
            }
            appareil.setConteneur(conteneur);
            appareil.setCamion(null);
        } else {
            Camion camion = camionRepository.findById(camionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));
            if (!Boolean.TRUE.equals(camion.getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Impossible de rattacher un appareil à un camion inactif");
            }
            appareil.setCamion(camion);
            appareil.setConteneur(null);
        }
    }

    // Import en masse depuis un CSV : une ligne par appareil, memes champs
    // que la creation unitaire (identifiantMateriel,typeAppareil,
    // conteneurId,camionId, en-tete attendu sur la premiere ligne).
    // Reutilise create(...) tel quel pour chaque ligne — meme validation
    // XOR/unicite/existence que la creation unitaire, rien duplique.
    //
    // Volontairement PAS @Transactional ici : create(...) est appele en
    // auto-invocation (this.create(...) dans la meme classe), qui ne passe
    // pas par le proxy Spring donc ignore de toute facon son @Transactional
    // — mais ca n'est pas un probleme puisque create() ne fait qu'un seul
    // save() (deja atomique via Spring Data). Sans transaction englobante,
    // chaque ligne reussie est committee independamment des autres :
    // l'echec d'une ligne ne peut pas faire annuler les lignes precedentes
    // ni bloquer les suivantes.
    public AppareilIotImportResultatDto importerCsv(MultipartFile fichier) {

        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier CSV vide ou manquant");
        }

        List<AppareilIotCreeResponseDto> crees = new ArrayList<>();
        List<AppareilIotImportLigneErreurDto> echecs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {

            String entete = reader.readLine();
            if (entete == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier CSV vide");
            }

            String ligneBrute;
            int numeroLigne = 1; // 1 = en-tete ; les donnees commencent a 2, comme dans un tableur
            while ((ligneBrute = reader.readLine()) != null) {
                numeroLigne++;
                if (ligneBrute.isBlank()) continue;

                // Split naif par virgule : suffisant pour ce format interne
                // (identifiants materiels/enums/ids, pas de texte libre avec
                // virgules a echapper) — pas de dependance a une lib CSV pour
                // ce seul besoin.
                String[] champs = ligneBrute.split(",", -1);
                String identifiantMateriel = champ(champs, 0);

                try {
                    if (identifiantMateriel.isBlank()) {
                        throw new IllegalArgumentException("identifiantMateriel manquant");
                    }

                    String typeBrut = champ(champs, 1);
                    TypeAppareilIot type;
                    try {
                        type = TypeAppareilIot.valueOf(typeBrut.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "typeAppareil invalide : \"" + typeBrut + "\" (attendu CAPTEUR_BENNE ou LECTEUR_RFID)");
                    }

                    Long conteneurId = parseIdOptionnel(champ(champs, 2), "conteneurId");
                    Long camionId = parseIdOptionnel(champ(champs, 3), "camionId");

                    AppareilIotRequestDto dto = new AppareilIotRequestDto(identifiantMateriel, type, conteneurId, camionId);
                    crees.add(create(dto));

                } catch (ResponseStatusException e) {
                    echecs.add(new AppareilIotImportLigneErreurDto(numeroLigne, identifiantMateriel, e.getReason()));
                } catch (IllegalArgumentException e) {
                    echecs.add(new AppareilIotImportLigneErreurDto(numeroLigne, identifiantMateriel, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire le fichier CSV");
        }

        return new AppareilIotImportResultatDto(crees, echecs);
    }

    private String champ(String[] champs, int index) {
        return index < champs.length ? champs[index].trim() : "";
    }

    private Long parseIdOptionnel(String valeur, String nomChamp) {
        if (valeur == null || valeur.isBlank()) return null;
        try {
            return Long.parseLong(valeur);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nomChamp + " invalide : \"" + valeur + "\" (doit être un nombre)");
        }
    }

    public AppareilIotResponseDto getById(Long id) {
        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));
        return toResponseDto(appareil);
    }

    public List<AppareilIotResponseDto> getAll() {
        return appareilIotRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id) {
        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));
        appareil.setActif(false);
        appareilIotRepository.save(appareil);
    }

    // Invalide la cle actuelle et en genere une nouvelle (rotation) : utile
    // si une cle a fuite, sans avoir a recreer tout l'enregistrement.
    @Transactional
    public AppareilIotCreeResponseDto regenererCle(Long id) {
        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));

        String cleEnClair = apiKeyHasher.genererCle();
        appareil.setCleApiHash(apiKeyHasher.hash(cleEnClair));
        appareilIotRepository.save(appareil);

        return new AppareilIotCreeResponseDto(appareil.getId(), appareil.getIdentifiantMateriel(), cleEnClair);
    }

    private AppareilIotResponseDto toResponseDto(AppareilIot a) {
        return new AppareilIotResponseDto(
                a.getId(),
                a.getIdentifiantMateriel(),
                a.getTypeAppareil(),
                a.getConteneur() != null ? a.getConteneur().getId() : null,
                a.getConteneur() != null ? a.getConteneur().getCode() : null,
                a.getCamion() != null ? a.getCamion().getId() : null,
                a.getCamion() != null ? a.getCamion().getImmatriculation() : null,
                a.getActif(),
                a.getCreatedAt()
        );
    }
}
