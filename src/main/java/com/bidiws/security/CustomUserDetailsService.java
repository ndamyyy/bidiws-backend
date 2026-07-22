package com.bidiws.security;

import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Pont entre l'entité métier Utilisateur et le contrat Spring Security.
 * On ne fait PAS implémenter UserDetails à l'entité JPA elle-même :
 * ça coupe le couplage entre le domaine et le framework de sécurité.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur avec l'email : " + email));

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .authorities(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()))
                .disabled(!Boolean.TRUE.equals(utilisateur.getActif()))
                .build();
    }
}
