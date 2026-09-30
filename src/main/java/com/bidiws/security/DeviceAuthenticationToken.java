package com.bidiws.security;

import com.bidiws.entity.AppareilIot;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication d'un appareil IoT (pas un compte utilisateur) : le
 * principal est l'AppareilIot lui-meme, resolu par
 * DeviceApiKeyAuthenticationFilter a partir du header X-Device-Api-Key.
 */
public class DeviceAuthenticationToken extends AbstractAuthenticationToken {

    private final AppareilIot appareil;

    public DeviceAuthenticationToken(AppareilIot appareil) {
        super(List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
        this.appareil = appareil;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return appareil;
    }
}
