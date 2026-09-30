package com.bidiws.security;

import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur avec l'email : " + email));

        return new CustomUserDetails(utilisateur);
    }

    // Utilisee pour resoudre l'utilisateur depuis le sujet d'un JWT deja
    // emis (ID, pas email — voir JwtService) : distincte de
    // loadUserByUsername, imposee par l'interface UserDetailsService pour
    // le flux de login initial (email saisi par l'utilisateur), qui reste
    // inchangee. Retourne CustomUserDetails directement (pas UserDetails) :
    // les appelants ont besoin de getId().
    public CustomUserDetails loadUserById(Long id) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur avec l'id : " + id));

        return new CustomUserDetails(utilisateur);
    }
}