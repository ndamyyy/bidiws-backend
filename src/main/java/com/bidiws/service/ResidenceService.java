package com.bidiws.service;

import com.bidiws.dto.residence.ResidenceRequestDto;
import com.bidiws.dto.residence.ResidenceResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Ville;
import com.bidiws.entity.Zone;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.VilleRepository;
import com.bidiws.repository.ZoneRepository;
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

    @Transactional
    public ResidenceResponseDto create(ResidenceRequestDto dto) {

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
    public ResidenceResponseDto update(Long id, ResidenceRequestDto dto) {

        Residence residence = residenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

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

    public ResidenceResponseDto getById(Long id) {
        Residence residence = residenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));
        return toResponseDto(residence);
    }

    public List<ResidenceResponseDto> getByVille(Long villeId) {
        return residenceRepository.findByVilleId(villeId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ResidenceResponseDto> getByZone(Long zoneId) {
        return residenceRepository.findByZoneId(zoneId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ResidenceResponseDto> getAll() {
        return residenceRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id) {
        Residence residence = residenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));
        residence.setActif(false);
        residenceRepository.save(residence);
    }

    private Zone resoudreZone(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));
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