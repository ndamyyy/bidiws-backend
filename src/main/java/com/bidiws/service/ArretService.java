package com.bidiws.service;

import com.bidiws.dto.arret.ArretConteneurResponseDto;
import com.bidiws.dto.arret.ArretIncidentRequestDto;
import com.bidiws.dto.arret.ArretRequestDto;
import com.bidiws.dto.arret.ArretResponseDto;
import com.bidiws.entity.Arret;
import com.bidiws.entity.ArretConteneur;
import com.bidiws.entity.Conteneur;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Tournee;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.StatutTournee;
import com.bidiws.repository.ArretConteneurRepository;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArretService {

    private final ArretRepository arretRepository;
    private final TourneeRepository tourneeRepository;
    private final ResidenceRepository residenceRepository;
    private final ConteneurRepository conteneurRepository;
    private final ArretConteneurRepository arretConteneurRepository;
    private final ArretDetectionService arretDetectionService;
    private final ArretConteneurDetectionService arretConteneurDetectionService;

    @Transactional
    public ArretResponseDto creer(ArretRequestDto dto) {

        Tournee tournee = tourneeRepository.findById(dto.tourneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));

        if (tournee.getStatut() == StatutTournee.TERMINEE || tournee.getStatut() == StatutTournee.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible d'ajouter un arrêt à une tournée terminée ou annulée");
        }

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        Arret arret = Arret.builder()
                .tournee(tournee)
                .residence(residence)
                .ordre(dto.ordre().shortValue())
                .statut(StatutArret.EN_ATTENTE)
                .nbConteneurs(dto.nbConteneurs() != null ? dto.nbConteneurs().shortValue() : null)
                .typesConteneurs(dto.typesConteneurs())
                .build();

        Arret saved = arretRepository.save(arret);

        genererDetailParConteneur(saved, dto.residenceId());

        return toResponseDto(saved);
    }

    // Une ligne ArretConteneur EN_ATTENTE par conteneur actif de la residence,
    // pour qu'IotDetectionService puisse suivre chaque bac individuellement
    // (cf. ArretConteneurDetectionService). Residence sans conteneur enregistre :
    // aucune ligne creee, comportement inchange — l'Arret seul fait foi,
    // retrocompatibilite totale.
    private void genererDetailParConteneur(Arret arret, Long residenceId) {
        List<Conteneur> conteneurs = conteneurRepository.findByResidenceIdAndActifTrue(residenceId);

        for (Conteneur conteneur : conteneurs) {
            ArretConteneur ligne = ArretConteneur.builder()
                    .arret(arret)
                    .conteneur(conteneur)
                    .statut(StatutArret.EN_ATTENTE)
                    .build();
            arretConteneurRepository.save(ligne);
        }
    }

    @Transactional
    public ArretResponseDto changerStatut(Long id, StatutArret nouveauStatut) {
        // Toute modification provenant de cette API REST est considérée
        // comme une validation humaine.
        // Les futures détections GPS, RFID ou CAPTEUR_BENNE utiliseront
        // également ArretDetectionService mais avec un autre ModeDetection.

        Arret saved = arretDetectionService.appliquerDetection(
                id, nouveauStatut, ModeDetection.VALIDATION_CHAUFFEUR, (short) 100
        );

        cascaderVersConteneurs(id, nouveauStatut);

        return toResponseDto(saved);
    }

    // Une validation manuelle du chauffeur doit se refleter sur le detail par
    // bac (GET /arrets/{id}/conteneurs), sinon un gardien y verrait "0/4
    // confirmes" alors que la collecte est faite. Reutilise
    // ArretConteneurDetectionService (pas de logique de transition dupliquee) :
    // son idempotence gere deja sans risque le cas d'un conteneur deja
    // confirme par un capteur (no-op silencieux, transitionAutorisee s'en
    // charge). Residence sans conteneurs enregistres : liste vide, rien a
    // faire (retrocompatibilite).
    private void cascaderVersConteneurs(Long arretId, StatutArret nouveauStatut) {
        List<ArretConteneur> lignes = arretConteneurRepository.findByArretId(arretId);
        for (ArretConteneur ligne : lignes) {
            arretConteneurDetectionService.appliquerDetection(
                    arretId, ligne.getConteneur().getId(), nouveauStatut, ModeDetection.VALIDATION_CHAUFFEUR, (short) 100);
        }
    }

    @Transactional
    public ArretResponseDto signalerIncident(Long id, ArretIncidentRequestDto dto) {
        Arret saved = arretDetectionService.signalerIncident(
                id, dto.descriptionIncident(), dto.photoIncidentUrl()
        );
        return toResponseDto(saved);
    }

    public ArretResponseDto getById(Long id) {
        Arret arret = arretRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));
        return toResponseDto(arret);
    }

    public List<ArretResponseDto> getByTournee(Long tourneeId) {
        return arretRepository.findByTourneeIdOrderByOrdreAsc(tourneeId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ArretResponseDto> getByResidence(Long residenceId) {
        return arretRepository.findByResidenceIdOrderByCreatedAtDesc(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    // Detail par bac (statut individuel + niveau de remplissage) : permet a
    // un gardien de voir "3/4 confirmes, reste le bac #4" plutot que le seul
    // statut agrege de l'Arret. Liste vide pour une residence sans conteneurs
    // enregistres (comportement attendu, pas une erreur).
    public List<ArretConteneurResponseDto> getConteneurs(Long arretId) {
        if (!arretRepository.existsById(arretId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable");
        }
        return arretConteneurRepository.findByArretId(arretId).stream()
                .map(this::toConteneurResponseDto)
                .toList();
    }

    private ArretResponseDto toResponseDto(Arret a) {
        return new ArretResponseDto(
                a.getId(),
                a.getTournee().getId(),
                a.getResidence().getId(),
                a.getResidence().getNom(),
                a.getOrdre() != null ? a.getOrdre().intValue() : null,
                a.getStatut(),
                a.getHeureEstimee(),
                a.getHeureApproche(),
                a.getHeureCollecte(),
                a.getScoreConfiance() != null ? a.getScoreConfiance().intValue() : null,
                a.getModeDetection(),
                a.getNbConteneurs() != null ? a.getNbConteneurs().intValue() : null,
                a.getTypesConteneurs(),
                a.getPoidsKg(),
                a.getIncident(),
                a.getDescriptionIncident(),
                a.getPhotoIncidentUrl()
        );
    }

    private ArretConteneurResponseDto toConteneurResponseDto(ArretConteneur ac) {
        Conteneur c = ac.getConteneur();
        return new ArretConteneurResponseDto(
                ac.getId(),
                c.getId(),
                c.getCode(),
                ac.getStatut(),
                ac.getModeDetection(),
                ac.getScoreConfiance() != null ? ac.getScoreConfiance().intValue() : null,
                ac.getHorodatageConfirmation(),
                c.getNiveauRemplissagePct() != null ? c.getNiveauRemplissagePct().intValue() : null
        );
    }
}