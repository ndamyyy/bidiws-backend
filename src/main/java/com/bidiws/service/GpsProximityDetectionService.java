package com.bidiws.service;

import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import com.bidiws.repository.ArretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detection automatique par proximite GPS : quand le camion entre dans le
 * rayon de detection d'une residence dont l'arret est encore EN_ATTENTE,
 * fait passer cet arret en COLLECTE_PROBABLE — jamais COLLECTE_CONFIRMEE,
 * la proximite ne prouve pas qu'une collecte a eu lieu (le camion peut
 * juste passer ou etre bloque dans le trafic), c'est un signal faible.
 *
 * Ne touche jamais ArretConteneur : la position GPS du camion ne permet pas
 * de savoir quel bac precis est concerne, contrairement a un capteur ou une
 * puce RFID. Passe uniquement par ArretDetectionService.appliquerDetection,
 * meme point d'entree que la validation manuelle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpsProximityDetectionService {

    private static final int RAYON_DETECTION_DEFAUT_METRES = 50;
    private static final short SCORE_CONFIANCE_GPS = 50;
    private static final double RAYON_TERRE_METRES = 6_371_000.0;

    private final ArretRepository arretRepository;
    private final ArretDetectionService arretDetectionService;

    public void verifierProximite(Long tourneeId, BigDecimal latitude, BigDecimal longitude) {
        List<Arret> arretsEnAttente = arretRepository.findByTourneeIdAndStatut(tourneeId, StatutArret.EN_ATTENTE);

        for (Arret arret : arretsEnAttente) {
            Residence residence = arret.getResidence();

            if (residence.getLatitude() == null || residence.getLongitude() == null) {
                continue;
            }

            double distanceMetres = calculerDistanceMetres(
                    latitude, longitude, residence.getLatitude(), residence.getLongitude());

            int rayonDetection = residence.getRayonDetection() != null
                    ? residence.getRayonDetection()
                    : RAYON_DETECTION_DEFAUT_METRES;

            if (distanceMetres <= rayonDetection) {
                log.info("Proximité GPS détectée : arrêt={}, résidence={}, distance={}m, rayon={}m",
                        arret.getId(), residence.getId(), Math.round(distanceMetres), rayonDetection);

                arretDetectionService.appliquerDetection(
                        arret.getId(), StatutArret.COLLECTE_PROBABLE, ModeDetection.GPS_AUTO, SCORE_CONFIANCE_GPS);
            }
        }
    }

    private static double calculerDistanceMetres(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double deltaPhi = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLambda = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAYON_TERRE_METRES * c;
    }
}
