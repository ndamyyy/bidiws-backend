package com.bidiws.service;

import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.event.PositionGpsEvent;
import com.bidiws.repository.ArretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reutilise le flux de position GPS deja envoye en continu par le camion
 * (pour la carte, sans rien demander au chauffeur) pour detecter la
 * proximite d'une residence et faire passer l'arret correspondant en
 * COLLECTE_PROBABLE automatiquement.
 *
 * Ne monte JAMAIS seul a COLLECTE_CONFIRMEE : une position GPS proche ne
 * prouve pas qu'une collecte a eu lieu (le camion peut juste passer, etre
 * bloque dans le trafic). C'est une limite physique, pas technique — pour
 * confirmer reellement, il faut le capteur (qui witness le geste) ou le
 * tap du chauffeur. D'ou le score de confiance modere : jamais suffisant
 * seul pour ArretDetectionService.transitionAutorisee de faire plus.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpsProximiteService {

    private static final short SCORE_CONFIANCE_GPS = 60;
    private static final double RAYON_TERRE_METRES = 6_371_000;

    private final ArretRepository arretRepository;
    private final ArretDetectionService arretDetectionService;

    // Indispensable : appelee depuis un @TransactionalEventListener(AFTER_COMMIT)
    // + @Async, donc sans transaction/session active. arret.getResidence() est
    // lazy (FetchType.LAZY) — sans @Transactional ici, ce serait exactement le
    // meme bug de LazyInitializationException que celui trouve et corrige sur
    // AppareilIotRepository.findByCleApiHash (cf. commit detection-iot), juste
    // invisible en test unitaire puisque Mockito ne simule aucune session Hibernate.
    // PAS readOnly : arretDetectionService.appliquerDetection() ecrit dans cette
    // meme transaction (propagation REQUIRED par defaut) — readOnly=true aurait
    // risque un flush Hibernate saute, donc une ecriture silencieusement perdue.
    @Transactional
    public void detecterProximite(PositionGpsEvent event) {

        List<Arret> arrets = arretRepository.findByTourneeIdOrderByOrdreAsc(event.tourneeId());

        for (Arret arret : arrets) {

            // Deja au statut cible ou au-dela : rien a refaire, evite de
            // recalculer/re-publier a chaque signal GPS (plusieurs par minute).
            if (arret.getStatut() == StatutArret.COLLECTE_PROBABLE
                    || arret.getStatut() == StatutArret.COLLECTE_CONFIRMEE
                    || arret.getStatut() == StatutArret.INCIDENT) {
                continue;
            }

            Residence residence = arret.getResidence();
            if (residence.getLatitude() == null || residence.getLongitude() == null
                    || residence.getRayonDetection() == null) {
                continue;
            }

            double distance = distanceMetres(event.latitude(), event.longitude(),
                    residence.getLatitude(), residence.getLongitude());

            if (distance > residence.getRayonDetection()) {
                continue;
            }

            log.info("Camion a {} m de la residence {} (rayon {} m) : passage en COLLECTE_PROBABLE",
                    Math.round(distance), residence.getId(), residence.getRayonDetection());

            try {
                arretDetectionService.appliquerDetection(
                        arret.getId(), StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, SCORE_CONFIANCE_GPS);
            } catch (Exception ex) {
                log.error("Échec de la détection GPS de proximité pour l'arrêt {}", arret.getId(), ex);
            }
        }
    }

    // Formule de Haversine : distance orthodromique entre deux points
    // (latitude/longitude en degres), suffisante a l'echelle d'une ville —
    // pas besoin de la precision d'une projection geodesique complete.
    private double distanceMetres(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double deltaPhi = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double deltaLambda = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAYON_TERRE_METRES * c;
    }
}
