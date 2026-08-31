package com.bidiws.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Génération et validation des tokens JWT.
 * Clé et durée de validité pilotées par application.properties
 * (bidiws.jwt.secret / bidiws.jwt.expiration).
 *
 * Le sujet du token est l'ID utilisateur, pas l'email : l'email peut
 * changer (ProfilPage) alors qu'un token deja emis reste valide jusqu'a
 * expiration — un sujet base sur l'email rendrait ce token orphelin
 * (loadUserByUsername(ancienEmail) echoue) des le changement, tuant la
 * session en cours silencieusement. L'ID ne change jamais.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${bidiws.jwt.secret}") String secret,
            @Value("${bidiws.jwt.expiration}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(CustomUserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(String.valueOf(userDetails.getId()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaim(token, Claims::getSubject));
    }

    public boolean isTokenValid(String token, CustomUserDetails userDetails) {
        try {
            final Long userId = extractUserId(token);
            return userId.equals(userDetails.getId()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
