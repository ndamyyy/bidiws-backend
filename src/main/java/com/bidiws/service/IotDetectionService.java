package com.bidiws.service;

import com.bidiws.dto.iot.DetectionIotRequestDto;
import com.bidiws.dto.iot.DetectionIotResponseDto;
import com.bidiws.dto.iot.MesureRemplissageRequestDto;
import com.bidiws.entity.AppareilIot;
import com.bidiws.entity.Arret;
import com.bidiws.entity.Conteneur;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.TypeAppareilIot;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ConteneurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Traduit une detection materielle (capteur colle a un bac, lecteur RFID
 * sur un camion) en transition, en passant par ArretConteneurDetectionService
 * (suivi par bac individuel — cf. migration V6) plutot que directement par
 * ArretDetectionService : une residence a plusieurs conteneurs, le premier
 * capteur qui se declenche ne doit confirmer QUE son bac, pas tout l'arret.
 */
@Service
@RequiredArgsConstructor
public class IotDetectionService {

    // Detection automatique : confiance elevee mais pas absolue (contrairement
    // a VALIDATION_CHAUFFEUR = 100, une action humaine explicite). A affiner
    // si plusieurs signaux IoT doivent un jour se departager.
    private static final short SCORE_CONFIANCE_IOT = 90;

    private final ArretRepository arretRepository;
    private final ConteneurRepository conteneurRepository;
    private final ArretConteneurDetectionService arretConteneurDetectionService;

    @Transactional
    public DetectionIotResponseDto enregistrerDetection(AppareilIot appareil, DetectionIotRequestDto dto) {

        Conteneur conteneur = resoudreConteneur(appareil, dto.rfidTag());

        if (!Boolean.TRUE.equals(conteneur.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce conteneur est inactif");
        }

        Arret arret = resoudreArretDuJour(conteneur.getResidence().getId());

        ModeDetection mode = appareil.getTypeAppareil() == TypeAppareilIot.LECTEUR_RFID
                ? ModeDetection.RFID
                : ModeDetection.CAPTEUR_BENNE;

        arretConteneurDetectionService.appliquerDetection(
                arret.getId(), conteneur.getId(), StatutArret.COLLECTE_CONFIRMEE, mode, SCORE_CONFIANCE_IOT);

        // Reference explicitement l'Arret a jour : sa propre confirmation
        // (si tous les conteneurs viennent d'etre confirmes) a pu se
        // produire a l'instant dans l'appel ci-dessus.
        Arret miseAJour = arretRepository.findById(arret.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));

        return new DetectionIotResponseDto(miseAJour.getId(), miseAJour.getStatut(), dto.horodatage());
    }

    @Transactional
    public void enregistrerMesureRemplissage(AppareilIot appareil, MesureRemplissageRequestDto dto) {
        Conteneur conteneur = appareil.getConteneur();
        if (conteneur == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet appareil n'est rattaché à aucun conteneur");
        }

        conteneur.setNiveauRemplissagePct(dto.niveauPct().shortValue());
        conteneur.setDerniereMesureRemplissage(dto.horodatage());
        conteneurRepository.save(conteneur);
    }

    // Scenario capteur embarque : le conteneur est directement connu (pas
    // besoin de tag). Scenario lecteur sur camion : le tag recu identifie
    // le conteneur scanne au passage.
    private Conteneur resoudreConteneur(AppareilIot appareil, String rfidTag) {
        if (appareil.getConteneur() != null) {
            return appareil.getConteneur();
        }
        if (appareil.getCamion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet appareil n'est rattaché à aucun conteneur ni camion");
        }
        if (rfidTag == null || rfidTag.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "rfidTag requis pour un lecteur monté sur camion");
        }
        return conteneurRepository.findByRfidTag(rfidTag)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun conteneur pour ce tag RFID"));
    }

    // Une liste (pas Optional) : rien n'empeche plusieurs tournees le meme
    // jour pour la meme residence (types de collecte differents). Priorite
    // au premier arret pas encore terminal ; a defaut le premier trouve.
    private Arret resoudreArretDuJour(Long residenceId) {
        List<Arret> arrets = arretRepository.findByResidenceIdAndTournee_DateTournee(residenceId, LocalDate.now());
        return arrets.stream()
                .filter(a -> a.getStatut() != StatutArret.COLLECTE_CONFIRMEE && a.getStatut() != StatutArret.INCIDENT)
                .findFirst()
                .or(() -> arrets.stream().findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun arrêt prévu aujourd'hui pour cette résidence"));
    }
}
