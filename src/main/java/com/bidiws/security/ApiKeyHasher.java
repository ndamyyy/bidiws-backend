package com.bidiws.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genere et hash les cles API des appareils IoT. SHA-256, pas BCrypt :
 * la cle est generee ici (haute entropie, 256 bits), pas choisie par un
 * humain, donc un hash rapide et deterministe suffit contre le vol de
 * dump — et surtout permet un lookup direct par hash (AppareilIotRepository
 * .findByCleApiHash), impossible avec un hash sale comme BCrypt puisque
 * la requete /iot/detections ne transporte que la cle, sans identifiant
 * device separe.
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
