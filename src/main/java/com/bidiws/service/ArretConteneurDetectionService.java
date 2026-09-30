package com.bidiws.service;

import com.bidiws.entity.ArretConteneur;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.repository.ArretConteneurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Fait la meme chose qu'ArretDetectionService, mais au niveau d'un bac
 * individuel plutot que de l'Arret entier : une residence a plusieurs
 * conteneurs, chacun a son propre ArretConteneur (cf. migration V6,
 * genere par ArretService.creer). L'Arret parent ne passe COLLECTE_CONFIRMEE
 * que quand TOUS ses ArretConteneur le sont — jusque-la, chaque capteur qui
 * se declenche met a jour SON bac sans toucher a l'Arret.
 *
 * Residence sans conteneurs enregistres (aucune ligne ArretConteneur pour
 * cet Arret) : retrocompatibilite totale, la detection s'applique
 * directement sur l'Arret via ArretDetectionService, exactement comme avant
 * l'introduction du suivi par conteneur.
 *
 * Ne remplace pas ArretDetectionService : le reutilise pour l'Arret parent
 * (meme point d'entree unique, meme chaine event -> notif -> WebSocket) et
 * pour le fallback retrocompat. ArretDetectionService.transitionAutorisee
 * n'est pas touche ; la meme regle est dupliquee ici volontairement, au
 * niveau du bac, puisque le "parent" et le "detail" ne sont pas la meme
 * ressource.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArretConteneurDetectionService {

    private final ArretConteneurRepository arretConteneurRepository;
    private final ArretDetectionService arretDetectionService;

    @Transactional
    public void appliquerDetection(Long arretId, Long conteneurId, StatutArret nouveauStatut,
                                    ModeDetection modeDetection, Short scoreConfiance) {

        Optional<ArretConteneur> ligneOpt = arretConteneurRepository.findByArretIdAndConteneurId(arretId, conteneurId);

        if (ligneOpt.isEmpty()) {
            arretDetectionService.appliquerDetection(arretId, nouveauStatut, modeDetection, scoreConfiance);
            return;
        }

        ArretConteneur ligne = ligneOpt.get();
        StatutArret ancienStatut = ligne.getStatut();

        if (!transitionAutorisee(ancienStatut, nouveauStatut)) {
            log.warn("Transition refusée sur ArretConteneur {} (arrêt {}, conteneur {}) : {} -> {}",
                    ligne.getId(), arretId, conteneurId, ancienStatut, nouveauStatut);
            return;
        }

        ligne.setStatut(nouveauStatut);
        ligne.setModeDetection(modeDetection);
        if (scoreConfiance != null) {
            ligne.setScoreConfiance(scoreConfiance);
        }
        if (nouveauStatut == StatutArret.COLLECTE_CONFIRMEE) {
            ligne.setHorodatageConfirmation(LocalDateTime.now());
        }

        arretConteneurRepository.save(ligne);

        log.info("ArretConteneur {} (arrêt {}, conteneur {}) : {} -> {}",
                ligne.getId(), arretId, conteneurId, ancienStatut, nouveauStatut);

        if (nouveauStatut == StatutArret.COLLECTE_CONFIRMEE && tousLesConteneursConfirmes(arretId)) {
            log.info("Tous les conteneurs de l'arrêt {} sont confirmés : confirmation de l'arrêt", arretId);
            arretDetectionService.appliquerDetection(arretId, StatutArret.COLLECTE_CONFIRMEE, modeDetection, scoreConfiance);
        }
    }

    private boolean tousLesConteneursConfirmes(Long arretId) {
        List<ArretConteneur> lignes = arretConteneurRepository.findByArretId(arretId);
        return !lignes.isEmpty() && lignes.stream().allMatch(l -> l.getStatut() == StatutArret.COLLECTE_CONFIRMEE);
    }

    // Meme regle qu'ArretDetectionService.transitionAutorisee, dupliquee
    // volontairement : celle-ci s'applique au detail par conteneur, celle
    // d'ArretDetectionService reste le seul point d'entree pour l'Arret
    // parent (non modifiee, cf. contrainte de la tache).
    private boolean transitionAutorisee(StatutArret ancien, StatutArret nouveau) {
        if (ancien == StatutArret.COLLECTE_CONFIRMEE || ancien == StatutArret.INCIDENT) {
            return false;
        }
        return ancien != nouveau;
    }
}
