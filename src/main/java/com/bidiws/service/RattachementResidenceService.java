package com.bidiws.service;

import com.bidiws.entity.Residence;
import com.bidiws.entity.Ville;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.VilleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Rattachement automatique d'un habitant a une residence a l'inscription,
 * a partir d'une adresse choisie via l'autocomplete API Adresse
 * (data.gouv.fr) sur RegisterPage — voir UtilisateurService.register,
 * seul appelant. Ne cree jamais de residence en dehors d'une ville deja
 * embarquee sur BIDIWS (table Ville) : c'est le garde-fou explicitement
 * demande, pas une simple validation de forme.
 *
 * Garde-fou anti-doublon : la comparaison se fait sur les coordonnees GPS
 * precises renvoyees par l'API Adresse (Haversine, meme formule que
 * GpsProximityDetectionService), jamais sur une comparaison de texte —
 * les accents, tirets, majuscules d'une adresse saisie librement rendent
 * une comparaison textuelle non fiable pour detecter un doublon.
 */
@Service
@RequiredArgsConstructor
public class RattachementResidenceService {

    // Rayon volontairement beaucoup plus strict que les 50m par defaut de
    // GpsProximityDetectionService (RAYON_DETECTION_DEFAUT_METRES) : ici
    // le but est de detecter la MEME residence (meme immeuble/parcelle),
    // pas la proximite d'un camion en tournee — un rayon large creerait
    // de faux rattachements entre residences voisines mais distinctes.
    private static final double RAYON_RATTACHEMENT_METRES = 15.0;
    private static final double RAYON_TERRE_METRES = 6_371_000.0;

    private final VilleRepository villeRepository;
    private final ResidenceRepository residenceRepository;

    // Resultat du rattachement : soit une Residence (existante trouvee a
    // proximite immediate, ou nouvellement creee), soit zoneNonCouverte
    // (aucune Ville embarquee ne correspond) — jamais les deux.
    public record Resultat(Residence residence, boolean zoneNonCouverte) {
        public static Resultat nonCouvert() {
            return new Resultat(null, true);
        }

        public static Resultat rattache(Residence residence) {
            return new Resultat(residence, false);
        }
    }

    @Transactional
    public Resultat rattacher(String adresse, String codePostal, String villeNom,
                               BigDecimal latitude, BigDecimal longitude) {

        Optional<Ville> villeOpt = villeRepository.findByNomIgnoreCase(villeNom);
        if (villeOpt.isEmpty()) {
            return Resultat.nonCouvert();
        }
        Ville ville = villeOpt.get();

        Residence residenceProche = residenceRepository.findByVilleIdAndActifTrue(ville.getId()).stream()
                .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
                .filter(r -> calculerDistanceMetres(latitude, longitude, r.getLatitude(), r.getLongitude())
                        <= RAYON_RATTACHEMENT_METRES)
                .findFirst()
                .orElse(null);

        if (residenceProche != null) {
            return Resultat.rattache(residenceProche);
        }

        // Aucune residence existante a proximite immediate : on en cree
        // une, avec les infos de l'adresse choisie. Pas de nom fourni par
        // ce flux (contrairement a la creation manuelle par un admin) :
        // l'adresse elle-meme sert de nom, prealable pratique qu'un
        // admin/syndic pourra renommer plus tard.
        Residence nouvelle = Residence.builder()
                .nom(adresse)
                .adresse(adresse)
                .codePostal(codePostal)
                .ville(ville)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        return Resultat.rattache(residenceRepository.save(nouvelle));
    }

    // Meme formule que GpsProximityDetectionService.calculerDistanceMetres
    // (Haversine) — dupliquee plutot que partagee : deux usages distincts
    // (detection de proximite camion vs deduplication residence a
    // l'inscription), pas de raison de les coupler via une dependance
    // commune pour quelques lignes de calcul geometrique pur.
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
