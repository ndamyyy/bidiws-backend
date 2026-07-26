package com.bidiws.service;

import com.bidiws.dto.camion.CamionRequestDto;
import com.bidiws.dto.camion.CamionResponseDto;
import com.bidiws.entity.Camion;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ChauffeurCamionRepository;
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

    @Transactional
    public CamionResponseDto create(CamionRequestDto dto) {

        if (camionRepository.existsByImmatriculation(dto.immatriculation())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette immatriculation existe déjà");
        }

        Camion camion = Camion.builder()
                .immatriculation(dto.immatriculation())
                .modele(dto.modele())
                .typeBenne(dto.typeBenne())
                .capaciteTonnes(dto.capaciteTonnes())
                .gpsActif(dto.gpsActif())
                .capteurBenne(dto.capteurBenne())
                .actif(true)
                .build();

        return toResponseDto(camionRepository.save(camion));
    }

    @Transactional
    public CamionResponseDto update(Long id, CamionRequestDto dto) {

        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));

        if (!camion.getImmatriculation().equals(dto.immatriculation())
                && camionRepository.existsByImmatriculation(dto.immatriculation())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette immatriculation existe déjà");
        }

        camion.setImmatriculation(dto.immatriculation());
        camion.setModele(dto.modele());
        camion.setTypeBenne(dto.typeBenne());
        camion.setCapaciteTonnes(dto.capaciteTonnes());
        camion.setGpsActif(dto.gpsActif());
        camion.setCapteurBenne(dto.capteurBenne());

        return toResponseDto(camionRepository.save(camion));
    }

    public CamionResponseDto getById(Long id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));
        return toResponseDto(camion);
    }

    public List<CamionResponseDto> getAll() {
        return camionRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<CamionResponseDto> getActifs() {
        return camionRepository.findByActifTrue().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));

        if (chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de désactiver : un chauffeur est actuellement affecté à ce camion");
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
                c.getActif()
        );
    }
}