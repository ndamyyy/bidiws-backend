package com.bidiws.security;

import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String motDePasse;
    private final boolean actif;
    private final List<GrantedAuthority> authorities;
    private final Role role;
    private final Long villeId;

    public CustomUserDetails(Utilisateur utilisateur) {
        this.id = utilisateur.getId();
        this.email = utilisateur.getEmail();
        this.motDePasse = utilisateur.getMotDePasse();
        this.actif = Boolean.TRUE.equals(utilisateur.getActif());
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
        this.role = utilisateur.getRole();
        this.villeId = utilisateur.getVille() != null ? utilisateur.getVille().getId() : null;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    // Non nul uniquement pour un compte MAIRIE rattache a une ville.
    public Long getVilleId() {
        return villeId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}