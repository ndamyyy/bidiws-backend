package com.bidiws.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Limite par IP, en complement (pas en remplacement) du verrouillage par
// compte deja en place dans AuthService : celui-ci ne protege pas contre le
// credential stuffing (un seul mot de passe teste sur des milliers de
// comptes differents) — aucun compte individuel n'atteint jamais son propre
// seuil dans ce scenario, puisque chaque compte n'est touche qu'une fois.
//
// Fenetre fixe (pas glissante) : un abus pile a la frontiere de la fenetre
// peut depasser legerement la limite nominale sur une courte periode, mais
// c'est un compromis acceptable ici (pas un rate-limit de facturation) pour
// une implementation simple, en memoire, sans dependance externe (Bucket4j
// aurait ajoute une dependance et, pour etre correct en cas de plusieurs
// instances, un backend partage type Redis — hors de proportion pour ce
// besoin sur une seule instance).
//
// En memoire uniquement : suffisant pour une seule instance de l'app
// (situation actuelle) ; a revoir (backend partage) si l'app est un jour
// deployee en plusieurs instances derriere un load balancer.
@Component
public class IpRateLimiter {

    static final int LIMITE_TENTATIVES = 20;
    static final Duration FENETRE = Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentHashMap<String, Fenetre> compteursParIp = new ConcurrentHashMap<>();

    public IpRateLimiter(Clock clock) {
        this.clock = clock;
    }

    // true si la tentative est autorisee (et comptabilisee), false si la
    // limite est deja atteinte pour cette IP sur la fenetre en cours.
    public boolean autoriser(String ip) {
        return compteursParIp.computeIfAbsent(ip, id -> new Fenetre())
                .enregistrerTentative(clock.instant());
    }

    private static final class Fenetre {
        private Instant debut;
        private int compte;

        synchronized boolean enregistrerTentative(Instant maintenant) {
            if (debut == null || Duration.between(debut, maintenant).compareTo(FENETRE) >= 0) {
                debut = maintenant;
                compte = 0;
            }
            compte++;
            return compte <= LIMITE_TENTATIVES;
        }
    }
}
