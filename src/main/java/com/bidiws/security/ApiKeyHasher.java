package com.bidiws.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genere et hash des jetons opaques haute entropie (cles API des
 * appareils IoT, refresh tokens). SHA-256, pas BCrypt : le jeton est
 * genere ici (haute entropie, 256 bits), pas choisi par un humain, donc
 * un hash rapide et deterministe suffit contre le vol de dump — et
 * surtout permet un lookup direct par hash (AppareilIotRepository
 * .findByCleApiHash, RefreshTokenRepository.findByTokenHash), impossible
 * avec un hash sale comme BCrypt puisque ces requetes ne transportent que
 * le jeton, sans identifiant separe.
 */
@Component
public class ApiKeyHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String genererCle() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String cleEnClair) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(cleEnClair.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
