package com.bidiws.service;

import com.bidiws.dto.residence.ResidenceRequestDto;
import com.bidiws.dto.residence.ResidenceResponseDto;
import com.bidiws.entity.*;
import com.bidiws.enums.Role;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ResidenceGardienRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.ResidenceSyndicRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.repository.ZoneRepository;
import com.bidiws.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResidenceService {

    private final ResidenceRepository residenceRepository;
    private final VilleRepository villeRepository;
    private final ZoneRepository zoneRepository;
    private final ResidenceGardienRepository residenceGardienRepository;
    private final ResidenceSyndicRepository residenceSyndicRepository;
    private final ArretRepository arretRepository;

    @Transactional
    public ResidenceResponseDto create(ResidenceRequestDto dto, CustomUserDetails userDetails) {

        if (userDetails.getRole() == Role.MAIRIE
                && (userDetails.getVilleId() == null || !userDetails.getVilleId().equals(dto.villeId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez créer une résidence que dans votre propre ville");
        }

        Ville ville = villeRepository.findById(dto.villeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        if (residenceRepository.existsByVilleIdAndAdresseIgnoreCase(dto.villeId(), dto.adresse())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une résidence avec cette adresse existe déjà dans cette ville");
        }

        Zone zone = resoudreZone(dto.zoneId(), dto.villeId());

        Residence residence = Residence.builder()
                .nom(dto.nom())
                .adresse(dto.adresse())
                .complement(dto.complement())
                .codePostal(dto.codePostal())
                .ville(ville)
                .zone(zone)
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .rayonDetection(dto.rayonDetection())
                .nbConteneurs(dto.nbConteneurs())
                .actif(true)
                .build();

        return toResponseDto(residenceRepository.save(residence));
    }

    @Transactional
    public ResidenceResponseDto update(Long id, ResidenceRequestDto dto, CustomUserDetails userDetails) {

        Residence residence = residenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        verifierAccesEcriture(residence, dto.villeId(), userDetails);

        Ville ville = villeRepository.findById(dto.villeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        boolean adresseChangee = !residence.getAdresse().equalsIgnoreCase(dto.adresse())
                || !residence.getVille().getId().equals(dto.villeId());

        if (adresseChangee && residenceRepository.existsByVilleIdAndAdresseIgnoreCase(dto.villeId(), dto.adresse())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une résidence avec cette adresse existe déjà dans cette ville");
        }

        Zone zone = resoudreZone(dto.zoneId(), dto.villeId());

        residence.setNom(dto.nom());
        residence.setAdresse(dto.adresse());
        residence.setComplement(dto.complement());
        residence.setCodePostal(dto.codePostal());
        residence.setVille(ville);
        residence.setZone(zone);
        residence.setLatitude(dto.latitude());
        residence.setLongitude(dto.longitude());
        residence.setRayonDetection(dto.rayonDetection());
        residence.setNbConteneurs(dto.nbConteneurs());

        return toResponseDto(residenceRepository.save(residence));
    }

    // ADMIN : illimite. MAIRIE : uniquement sa propre ville (et ne peut pas
    // faire basculer la residence vers une autre ville). SYNDIC : uniquement
    // les residences qu'il gere, sans restriction de ville.
    private void verifierAccesEcriture(Residence residence, Long villeIdCible, CustomUserDetails userDetails) {
        switch (userDetails.getRole()) {
            case ADMIN -> { }
            case MAIRIE -> {
                if (userDetails.getVilleId() == null
                        || !residence.getVille().getId().equals(userDetails.getVilleId())
                        || !villeIdCible.equals(userDetails.getVilleId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Vous ne pouvez modifier que les résidences de votre ville");
                }
            }
            case SYNDIC -> {
                if (!residenceSyndicRepository.existsByResidenceIdAndSyndicId(residence.getId(), userDetails.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne gérez pas cette résidence");
                }
                if (!residence.getVille().getId().equals(villeIdCible)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Un syndic ne peut pas changer la ville d'une résidence");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }
    }

    private Zone resoudreZone(Long zoneId, Long villeId) {
        if (zoneId == null) {
            return null;
        }
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));

        if (!zone.getVille().getId().equals(villeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette zone n'appartient pas à la ville sélectionnée");
        }

        return zone;
    }

    public ResidenceResponseDto getById(Long id, CustomUserDetails userDetails) {
        Residence residence = residenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));
        verifierAccesLecture(residence, userDetails);
        return toResponseDto(residence);
    }

    private void verifierAccesLecture(Residence residence, CustomUserDetails userDetails) {
        boolean autorise = switch (userDetails.getRole()) {
            case ADMIN -> true;
            case MAIRIE -> userDetails.getVilleId() != null
                    && residence.getVille().getId().equals(userDetails.getVilleId());
            case SYNDIC -> residenceSyndicRepository.existsByResidenceIdAndSyndicId(residence.getId(), userDetails.getId());
            case GARDIEN -> residenceGardienRepository.existsByResidenceIdAndGardienId(residence.getId(), userDetails.getId());
            default -> false;
        };
        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé à cette résidence");
        }
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> getGardiens(Long residenceId) {

        Residence residence = residenceRepository.findById(residenceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Résidence introuvable"));

        return residence.getGardiens()
                .stream()
                .map(ResidenceGardien::getGardien)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> getHabitants(Long residenceId) {

        Residence residence = residenceRepository.findById(residenceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Résidence introuvable"));

        return residence.getHabitants()
                .stream()
                .map(ResidenceHabitant::getHabitant)
                .toList();
    }

    // Base scopee par role, puis affinee par les filtres optionnels
    // villeId/zoneId (utiles surtout pour ADMIN, qui n'a pas de scope de base).
    public List<ResidenceResponseDto> getAll(CustomUserDetails userDetails, Long villeId, Long zoneId) {

        List<Residence> base = switch (userDetails.getRole()) {
            case ADMIN -> residenceRepository.findAll();
            case MAIRIE -> userDetails.getVilleId() != null
                    ? residenceRepository.findByVilleId(userDetails.getVilleId())
                    : List.of();
            case SYNDIC -> residenceSyndicRepository.findBySyndicId(userDetails.getId()).stream()
                    .map(ResidenceSyndic::getResidence)
                    .toList();
            case GARDIEN -> residenceGardienRepository.findByGardienId(userDetails.getId()).stream()
                    .map(ResidenceGardien::getResidence)
                    .toList();
            default -> List.of();
        };

        return base.stream()
                .filter(r -> villeId == null || r.getVille().getId().equals(villeId))
                .filter(r -> zoneId == null || (r.getZone() != null && r.getZone().getId().equals(zoneId)))
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id, CustomUserDetails userDetails) {
        Residence residence = residenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        boolean autorise = userDetails.getRole() == Role.ADMIN
                || (userDetails.getRole() == Role.MAIRIE
                        && userDetails.getVilleId() != null
                        && residence.getVille().getId().equals(userDetails.getVilleId()));

        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }

        if (arretRepository.existsActifByResidenceId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de désactiver : un arrêt de cette résidence est sur une tournée active");
        }

        residence.setActif(false);
        residenceRepository.save(residence);
    }


    private ResidenceResponseDto toResponseDto(Residence r) {
        return new ResidenceResponseDto(
                r.getId(),
                r.getNom(),
                r.getAdresse(),
                r.getComplement(),
                r.getCodePostal(),
                r.getVille().getId(),
                r.getVille().getNom(),
                r.getZone() != null ? r.getZone().getId() : null,
                r.getZone() != null ? r.getZone().getNom() : null,
                r.getLatitude(),
                r.getLongitude(),
                r.getRayonDetection(),
                r.getNbConteneurs(),
                r.getActif()
        );
    }
}