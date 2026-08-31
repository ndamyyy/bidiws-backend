package com.bidiws.security;

import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le sujet du JWT est l'ID utilisateur, pas l'email (voir JwtService) :
 * un changement d'email (ProfilPage) ne doit jamais invalider un token
 * deja emis. Regression couverte explicitement ici.
 */
class JwtServiceTest {

    // >= 32 octets, exige par Keys.hmacShaKeyFor pour HS256.
    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hs256";
    private static final long EXPIRATION_MS = 3_600_000L;
    private static final Long USER_ID = 42L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS);

    private CustomUserDetails utilisateur(Long id, String email) {
        return new CustomUserDetails(Utilisateur.builder()
                .id(id)
                .email(email)
                .motDePasse("hash")
                .role(Role.HABITANT)
                .actif(true)
                .build());
    }

    @Test
    void leSujetDuTokenEstLIdUtilisateurPasLEmail() {
        String token = jwtService.generateToken(utilisateur(USER_ID, "avant@bidiws.com"));

        assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    void leTokenResteValideApresUnChangementDEmailMemeIdMemeToken() {
        // Genere avec l'ancien email...
        String token = jwtService.generateToken(utilisateur(USER_ID, "avant@bidiws.com"));

        // ...puis rejoue le MEME token contre l'utilisateur tel qu'il est
        // APRES le changement d'email (meme id, nouvel email) : simule
        // exactement refreshUtilisateur() juste apres une modification de
        // profil reussie, sans nouvelle connexion.
        CustomUserDetails utilisateurApresChangementEmail = utilisateur(USER_ID, "apres@bidiws.com");

        assertThat(jwtService.isTokenValid(token, utilisateurApresChangementEmail)).isTrue();
    }

    @Test
    void leTokenEstInvalidePourUnAutreUtilisateur() {
        String token = jwtService.generateToken(utilisateur(USER_ID, "user@bidiws.com"));

        CustomUserDetails autreUtilisateur = utilisateur(99L, "user@bidiws.com");

        assertThat(jwtService.isTokenValid(token, autreUtilisateur)).isFalse();
    }
}
