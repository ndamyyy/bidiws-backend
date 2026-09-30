package com.bidiws.service;

import com.bidiws.entity.RefreshToken;
import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.RefreshTokenRepository;
import com.bidiws.security.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh tokens : jeton opaque (pas un JWT) stocke en base sous forme de
 * hash — doit pouvoir etre revoque avant expiration, ce qu'un JWT
 * stateless ne permet pas. A usage unique (rotation) : chaque appel a
 * /auth/refresh consomme le token presente et en emet un nouveau dans la
 * meme famille. Voir V9__refresh_token.sql pour le raisonnement complet
 * sur la revocation de famille en cas de reutilisation detectee.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${bidiws.refresh-token.expiration-days:30}")
    private int expirationJours;

    private final RefreshTokenRepository refreshTokenRepository;
    private final ApiKeyHasher tokenHasher;

    // Login : premier maillon d'une nouvelle chaine de rotation.
    @Transactional
    public String creerNouvelleFamille(Utilisateur utilisateur) {
        return creerToken(utilisateur, UUID.randomUUID());
    }

    @Transactional
    public String creerToken(Utilisateur utilisateur, UUID familleId) {
        String tokenEnClair = tokenHasher.genererCle();

        RefreshToken token = RefreshToken.builder()
                .utilisateur(utilisateur)
                .tokenHash(tokenHasher.hash(tokenEnClair))
                .familleId(familleId)
                .expireA(LocalDateTime.now().plusDays(expirationJours))
                .build();

        refreshTokenRepository.save(token);
        return tokenEnClair;
    }

    // Verifie le refresh token presente et le marque consomme (usage
    // unique). Ne cree PAS le token suivant ici : l'appelant (AuthService)
    // reste responsable de la generation de la paire complete, comme pour
    // le login. Retourne l'entite (pas juste l'utilisateur) : l'appelant a
    // besoin de familleId pour poursuivre la meme chaine de rotation.
    //
    // @Transactional requis (pas optionnel comme pour de simples save()) :
    // revoquerFamille() est une requete @Modifying en masse, qui exige une
    // transaction deja active pour s'executer (contrairement a save(), qui
    // est individuellement transactionnel sur SimpleJpaRepository) — sans
    // @Transactional ici, Hibernate refuse carrement l'UPDATE
    // (TransactionRequiredException), constate en conditions reelles.
    //
    // noRollbackFor=ResponseStatusException.class : cette methode se termine
    // TOUJOURS par une exception relancee sur les chemins d'echec (les 401),
    // y compris juste apres revoquerFamille() — sans ce noRollbackFor, le
    // rollback automatique sur RuntimeException annulerait l'UPDATE que l'on
    // vient de faire. Bug reproduit puis corrige en conditions reelles :
    // sans lui, la reutilisation etait bien rejetee (401) mais la famille
    // n'etait PAS reellement revoquee en base, laissant le maillon suivant
    // toujours utilisable.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public RefreshToken consommerPourRotation(String refreshTokenEnClair) {
        String hash = tokenHasher.hash(refreshTokenEnClair);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide"));

        if (Boolean.TRUE.equals(token.getRevoque())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide");
        }

        if (token.getUtiliseA() != null) {
            // Reutilisation d'un token deja consomme : signal de vol
            // potentiel (attaquant et utilisateur legitime disposent tous
            // deux d'un maillon de la meme chaine). Revoque TOUTE la
            // famille, pas seulement cette requete : un maillon plus recent
            // deja obtenu par l'attaquant resterait sinon valide.
            refreshTokenRepository.revoquerFamille(token.getFamilleId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide");
        }

        if (token.getExpireA().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expiré");
        }

        if (!Boolean.TRUE.equals(token.getUtilisateur().getActif())) {
            // Meme message generique que pour un token invalide : ne pas
            // reveler qu'un compte existe mais est desactive, meme
            // raisonnement que le 401 generique du login.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide");
        }

        token.setUtiliseA(LocalDateTime.now());
        refreshTokenRepository.save(token);

        return token;
    }
}
