package com.bidiws.security;

import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.SignalementRepository;
import com.bidiws.repository.TourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final TourneeRepository tourneeRepository;
    private final ArretRepository arretRepository;
    private final SignalementRepository signalementRepository;

    public boolean isAssignedChauffeur(Long tourneeId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || !hasAuthority(authentication, "ROLE_CHAUFFEUR")) {
            return false;
        }

        return tourneeRepository.existsByIdAndChauffeurId(tourneeId, userDetails.getId());
    }

    public boolean isAssignedChauffeurForArret(Long arretId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || !hasAuthority(authentication, "ROLE_CHAUFFEUR")) {
            return false;
        }
        return arretRepository.existsByIdAndTourneeChauffeurId(arretId, userDetails.getId());
    }

    public boolean canAccessSignalement(Long signalementId, Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_ADMIN", "ROLE_MAIRIE")) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }
        if (signalementRepository.existsByIdAndAuteurId(signalementId, userDetails.getId())) {
            return true;
        }
        // Un gardien ne peut consulter que les signalements des residences
        // qu'il garde reellement, pas n'importe lequel.
        return hasAuthority(authentication, "ROLE_GARDIEN")
                && signalementRepository.existsByIdAndResidence_Gardiens_Gardien_Id(signalementId, userDetails.getId());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        for (String authority : authorities) {
            if (hasAuthority(authentication, authority)) {
                return true;
            }
        }
        return false;
    }
}
