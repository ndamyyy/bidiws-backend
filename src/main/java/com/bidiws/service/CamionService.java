package com.bidiws.service;

import com.bidiws.dto.camion.CamionRequestDto;
import com.bidiws.dto.camion.CamionResponseDto;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Ville;
import com.bidiws.enums.Role;
import com.bidiws.enums.StatutTournee;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ChauffeurCamionRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CamionService {

    private final CamionRepository camionRepository;
    private final ChauffeurCamionRepository chauffeurCamionRepository;
    private final TourneeRepository tourneeRepository;
    private final VilleRepository villeRepository;

    @Transactional
    public CamionResponseDto create(CamionRequestDto dto, CustomUserDetails userDetails) {

        if (userDetails.getRole() == Role.MAIRIE
                && (userDetails.getVilleId() == null || !userDetails.getVilleId().equals(dto.villeId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez créer un camion que dans votre propre ville");
        }

        if (camionRepository.existsByImmatriculation(dto.immatriculation())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette immatriculation existe déjà");
        }

        Ville ville = villeRepository.findById(dto.villeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        Camion camion = Camion.builder()
                .immatriculation(dto.immatriculation())
                .modele(dto.modele())
                .typeBenne(dto.typeBenne())
                .capaciteTonnes(dto.capaciteTonnes())
                .gpsActif(dto.gpsActif())
                .capteurBenne(dto.capteurBenne())
                .ville(ville)
                .actif(true)
                .build();

        return toResponseDto(camionRepository.save(camion));
    }

    @Transactional
    public CamionResponseDto update(Long id, CamionRequestDto dto, CustomUserDetails userDetails) {

        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));

        verifierAccesEcriture(camion, dto.villeId(), userDetails);

        if (!camion.getImmatriculation().equals(dto.immatriculation())
                && camionRepository.existsByImmatriculation(dto.immatriculation())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette immatriculation existe déjà");
        }

        Ville ville = villeRepository.findById(dto.villeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        camion.setImmatriculation(dto.immatriculation());
        camion.setModele(dto.modele());
        camion.setTypeBenne(dto.typeBenne());
        camion.setCapaciteTonnes(dto.capaciteTonnes());
        camion.setGpsActif(dto.gpsActif());
        camion.setCapteurBenne(dto.capteurBenne());
        camion.setVille(ville);

        return toResponseDto(camionRepository.save(camion));
    }

    // ADMIN : illimite. MAIRIE : uniquement les camions de sa propre ville
    // (et ne peut pas faire basculer un camion vers une autre ville), meme
    // regle fail-closed que ResidenceService.verifierAccesEcriture.
    private void verifierAccesEcriture(Camion camion, Long villeIdCible, CustomUserDetails userDetails) {
        if (userDetails.getRole() == Role.ADMIN) {
            return;
        }
        if (userDetails.getRole() != Role.MAIRIE
                || userDetails.getVilleId() == null
                || camion.getVille() == null
                || !camion.getVille().getId().equals(userDetails.getVilleId())
                || !villeIdCible.equals(userDetails.getVilleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez gérer que les camions de votre ville");
        }
    }

    public CamionResponseDto getById(Long id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));
        return toResponseDto(camion);
    }

    // Base scopee par role (ADMIN: tout, MAIRIE: sa ville uniquement, fail-closed
    // si pas de ville rattachee), puis affinee par le filtre optionnel "actif".
    public List<CamionResponseDto> getAll(CustomUserDetails userDetails, Boolean actif) {

        List<Camion> base = switch (userDetails.getRole()) {
            case ADMIN -> camionRepository.findAll();
            case MAIRIE -> userDetails.getVilleId() != null
                    ? camionRepository.findByVilleId(userDetails.getVilleId())
                    : List.of();
            default -> List.of();
        };

        return base.stream()
                .filter(c -> actif == null || actif.equals(c.getActif()))
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id, CustomUserDetails userDetails) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));

        boolean autorise = userDetails.getRole() == Role.ADMIN
                || (userDetails.getRole() == Role.MAIRIE
                        && userDetails.getVilleId() != null
                        && camion.getVille() != null
                        && camion.getVille().getId().equals(userDetails.getVilleId()));

        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }

        if (chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de désactiver : un chauffeur est actuellement affecté à ce camion");
        }

        if (tourneeRepository.existsByCamionIdAndStatut(id, StatutTournee.EN_COURS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de désactiver : ce camion est sur une tournée en cours");
        }

        camion.setActif(false);
        camionRepository.save(camion);
    }

    private CamionResponseDto toResponseDto(Camion c) {
        return new CamionResponseDto(
                c.getId(),
                c.getImmatriculation(),
                c.getModele(),
                c.getTypeBenne(),
                c.getCapaciteTonnes(),
                c.getGpsActif(),
                c.getCapteurBenne(),
                c.getActif(),
                c.getVille() != null ? c.getVille().getId() : null,
                c.getVille() != null ? c.getVille().getNom() : null
        );
    }
}
