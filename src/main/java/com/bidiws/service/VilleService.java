package com.bidiws.service;

import com.bidiws.dto.ville.VilleRequestDto;
import com.bidiws.dto.ville.VilleResponseDto;
import com.bidiws.entity.Ville;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.UtilisateurRepository;
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
public class VilleService {

    private final VilleRepository villeRepository;
    private final ZoneRepository zoneRepository;
    private final CamionRepository camionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResidenceRepository residenceRepository;

    @Transactional
    public VilleResponseDto create(VilleRequestDto dto) {

        if (villeRepository.existsByNom(dto.nom())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette ville existe déjà");
        }
        if (villeRepository.existsByCodePostal(dto.codePostal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code postal existe déjà");
        }

        Ville ville = Ville.builder()
                .nom(dto.nom())
                .codePostal(dto.codePostal())
                .departement(dto.departement())
                .build();

        return toResponseDto(villeRepository.save(ville));
    }

    @Transactional
    public VilleResponseDto update(Long id, VilleRequestDto dto) {

        Ville ville = villeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        if (!ville.getNom().equals(dto.nom()) && villeRepository.existsByNom(dto.nom())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette ville existe déjà");
        }
        if (!ville.getCodePostal().equals(dto.codePostal()) && villeRepository.existsByCodePostal(dto.codePostal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code postal existe déjà");
        }

        ville.setNom(dto.nom());
        ville.setCodePostal(dto.codePostal());
        ville.setDepartement(dto.departement());

        return toResponseDto(villeRepository.save(ville));
    }

    public VilleResponseDto getById(Long id) {
        Ville ville = villeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));
        return toResponseDto(ville);
    }

    public List<VilleResponseDto> getAll() {
        return villeRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Ville ville = villeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        if (!zoneRepository.findByVilleId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : des zones sont rattachées à cette ville");
        }

        // residence.zone_id est nullable (V1) : une residence sans zone
        // rattachee ne serait pas bloquee par le check zones ci-dessus, d'ou
        // ce check separe sur residence.ville_id (toujours renseigne).
        if (residenceRepository.existsByVilleId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : des résidences sont rattachées à cette ville");
        }

        if (camionRepository.existsByVilleId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : des camions sont rattachés à cette ville");
        }

        if (utilisateurRepository.existsByVilleId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : des comptes MAIRIE sont rattachés à cette ville");
        }

        villeRepository.deleteById(ville.getId());
    }

    private VilleResponseDto toResponseDto(Ville v) {
        return new VilleResponseDto(
                v.getId(),
                v.getNom(),
                v.getCodePostal(),
                v.getDepartement()
        );
    }
}