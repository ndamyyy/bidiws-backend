package com.bidiws.service;

import com.bidiws.entity.Arret;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.enums.StatutTournee;
import com.bidiws.event.ArretStatutChangeEvent;
import com.bidiws.repository.ArretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Point d'entrée UNIQUE pour toute transition d'état d'un Arret, quelle que
 * soit la source de détection (validation manuelle du chauffeur, GPS
 * automatique, capteur de levage de benne, RFID, signalement communautaire...).
 *
 * Les contrôleurs et les futurs listeners (GPS, capteur) ne doivent JAMAIS
 * modifier un Arret directement : ils se contentent de transmettre
 * l'information ici, sous la forme d'une transition attribuée à un
 * {@link ModeDetection}. Toute la logique métier (règles de transition,
 * score de confiance, horodatage, publication de l'événement) reste
 * centralisée dans cette classe.
 *
 * Ajouter un nouveau mode de détection (ex. futur capteur) ne nécessite donc
 * aucune modification de cette classe ni des couches en aval (notifications,
 * WebSocket) : il suffit d'appeler {@link #appliquerDetection} avec le bon
 * {@link ModeDetection}.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ArretDetectionService {

    private final ArretRepository arretRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Enregistre une détection pour un arrêt et fait éventuellement transiter
     * son statut. Publie {@link ArretStatutChangeEvent} uniquement si le
     * statut change réellement.
     *
     * @param arretId        l'arrêt concerné
     * @param nouveauStatut  le statut proposé par la source de détection
     * @param modeDetection  la source à l'origine de cette détection
     * @param scoreConfiance score de confiance (0-100) associé à la détection ;
     *                       peut être {@code null} si la source ne le fournit pas
     * @return l'arrêt à jour (inchangé si la transition a été ignorée)
     */
    @Transactional
    public Arret appliquerDetection(Long arretId,
                                      StatutArret nouveauStatut,
                                      ModeDetection modeDetection,
                                      Short scoreConfiance) {

        Arret arret = arretRepository.findById(arretId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));

        if (!tourneeActive(arret)) {
            log.warn(
                    "Détection ignorée : tournée {} n'est plus active (statut {}) pour l'arrêt {}",
                    arret.getTournee().getId(),
                    arret.getTournee().getStatut(),
                    arretId
            );
            return arret;
        }

        log.info(
                "Détection reçue : arrêt={}, mode={}, statut={}",
                arretId,
                modeDetection,
                nouveauStatut
        );

        StatutArret ancienStatut = arret.getStatut();

        if (nouveauStatut == StatutArret.INCIDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisez le signalement d'incident dédié (POST /arrets/{id}/incident) pour ce statut");
        }

        if (!transitionAutorisee(ancienStatut, nouveauStatut)) {

            log.warn(
                    "Transition refusée : {} -> {} pour l'arrêt {}",
                    ancienStatut,
                    nouveauStatut,
                    arretId
            );

            return arret;
        }

        arret.setStatut(nouveauStatut);
        arret.setModeDetection(modeDetection);

        if (scoreConfiance != null) {

            if (scoreConfiance < 0 || scoreConfiance > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le score de confiance doit être compris entre 0 et 100");
            }

            arret.setScoreConfiance(scoreConfiance);
        }

        LocalDateTime maintenant = LocalDateTime.now();
        if (nouveauStatut == StatutArret.EN_APPROCHE && arret.getHeureApproche() == null) {
            arret.setHeureApproche(maintenant);
        }
        if (nouveauStatut == StatutArret.COLLECTE_CONFIRMEE && arret.getHeureCollecte() == null) {
            arret.setHeureCollecte(maintenant);
        }

        Arret saved = arretRepository.save(arret);

        log.info(
                "Arrêt {} passé de {} à {}",
                arretId,
                ancienStatut,
                nouveauStatut
        );

        eventPublisher.publishEvent(new ArretStatutChangeEvent(
                saved.getId(),
                saved.getTournee().getId(),
                saved.getResidence().getId(),
                ancienStatut,
                nouveauStatut
        ));

        return saved;
    }

    /**
     * Signalement d'incident explicite (action humaine), distinct d'une
     * détection automatique : contrairement à {@link #appliquerDetection},
     * cette transition n'est jamais refusée par {@link #transitionAutorisee} —
     * un incident réellement constaté prime sur un statut déjà confirmé.
     */
    @Transactional
    public Arret signalerIncident(Long arretId, String descriptionIncident, String photoIncidentUrl) {

        Arret arret = arretRepository.findById(arretId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));

        if (!tourneeActive(arret)) {
            log.warn(
                    "Signalement d'incident ignoré : tournée {} n'est plus active (statut {}) pour l'arrêt {}",
                    arret.getTournee().getId(),
                    arret.getTournee().getStatut(),
                    arretId
            );
            return arret;
        }

        StatutArret ancienStatut = arret.getStatut();

        arret.setStatut(StatutArret.INCIDENT);
        arret.setModeDetection(ModeDetection.VALIDATION_CHAUFFEUR);
        arret.setIncident(true);
        arret.setDescriptionIncident(descriptionIncident);
        arret.setPhotoIncidentUrl(photoIncidentUrl);

        Arret saved = arretRepository.save(arret);

        log.info("Incident signalé sur l'arrêt {} (ancien statut {})", arretId, ancienStatut);

        eventPublisher.publishEvent(new ArretStatutChangeEvent(
                saved.getId(),
                saved.getTournee().getId(),
                saved.getResidence().getId(),
                ancienStatut,
                StatutArret.INCIDENT
        ));

        return saved;
    }


    /**
     * Règle de transition centralisée : un arrêt déjà confirmé ou en incident
     * ne peut plus être rétrogradé par une détection automatique (GPS,
     * capteur, RFID...). Seule une action explicite (signalement d'incident,
     * correction manuelle) passe par un autre chemin que celui-ci.
     *
     * À affiner au fil de l'intégration des nouvelles sources (ex. ignorer
     * une détection GPS de score trop faible), mais le principe reste :
     * cette méthode est le SEUL endroit où cette règle est écrite.
     */
    private boolean transitionAutorisee(StatutArret ancien, StatutArret nouveau) {
        if (ancien == StatutArret.COLLECTE_CONFIRMEE || ancien == StatutArret.INCIDENT) {
            return false;
        }
        return ancien != nouveau;
    }

    // Garde-fou en amont de transitionAutorisee, pas dedans : une tournee
    // TERMINEE/ANNULEE bloque TOUTE modification d'arret (detection normale
    // comme signalement d'incident), pas seulement les transitions de statut
    // d'arret entre elles. Jamais d'exception ici : ce point est appele aussi
    // bien depuis un contexte utilisateur (chauffeur) que device/automatique
    // (capteur, GPS) qui ne doivent jamais planter sur un etat obsolete.
    private boolean tourneeActive(Arret arret) {
        StatutTournee statutTournee = arret.getTournee().getStatut();
        return statutTournee == StatutTournee.PLANIFIEE || statutTournee == StatutTournee.EN_COURS;
    }
}