package com.bidiws.service;

import com.bidiws.dto.zone.ZoneRequestDto;
import com.bidiws.dto.zone.ZoneResponseDto;
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
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final VilleRepository villeRepository;
    private final ResidenceRepository residenceRepository;

    @Transactional
    public ZoneResponseDto create(ZoneRequestDto dto) {

        Ville ville = villeRepository.findById(dto.villeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        if (zoneRepository.existsByVilleIdAndCode(dto.villeId(), dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code de zone existe déjà pour cette ville");
        }

        Zone zone = Zone.builder()
                .ville(ville)
                .nom(dto.nom())
                .code(dto.code())
                .description(dto.description())
                .build();

        return toResponseDto(zoneRepository.save(zone));
    }

    @Transactional
    public ZoneResponseDto update(Long id, ZoneRequestDto dto) {

        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));

        Ville ville = villeRepository.findById(dto.villeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable"));

        boolean codeChange = !zone.getCode().equals(dto.code()) || !zone.getVille().getId().equals(dto.villeId());
        if (codeChange && zoneRepository.existsByVilleIdAndCode(dto.villeId(), dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code de zone existe déjà pour cette ville");
        }

        zone.setVille(ville);
        zone.setNom(dto.nom());
        zone.setCode(dto.code());
        zone.setDescription(dto.description());

        return toResponseDto(zoneRepository.save(zone));
    }

    public ZoneResponseDto getById(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));
        return toResponseDto(zone);
    }

    public List<ZoneResponseDto> getByVille(Long villeId) {
        return zoneRepository.findByVilleId(villeId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ZoneResponseDto> getAll() {
        return zoneRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));

        if (!residenceRepository.findByZoneId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : des résidences sont rattachées à cette zone");
        }

        zoneRepository.deleteById(zone.getId());
    }

    private ZoneResponseDto toResponseDto(Zone z) {
        return new ZoneResponseDto(
                z.getId(),
                z.getVille().getId(),
                z.getVille().getNom(),
                z.getNom(),
                z.getCode(),
                z.getDescription()
        );
    }
}