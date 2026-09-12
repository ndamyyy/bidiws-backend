package com.bidiws.service;

import com.bidiws.dto.conteneur.ConteneurRequestDto;
import com.bidiws.dto.conteneur.ConteneurResponseDto;
import com.bidiws.entity.Conteneur;
import com.bidiws.entity.Residence;
import com.bidiws.repository.ArretConteneurRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.repository.ResidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConteneurService {

    private final ConteneurRepository conteneurRepository;
    private final ResidenceRepository residenceRepository;
    private final ArretConteneurRepository arretConteneurRepository;

    @Transactional
    public ConteneurResponseDto create(ConteneurRequestDto dto) {

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        if (!Boolean.TRUE.equals(residence.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette résidence est désactivée");
        }

        if (conteneurRepository.existsByResidenceIdAndCode(dto.residenceId(), dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un conteneur avec ce code existe déjà pour cette résidence");
        }

        Conteneur conteneur = Conteneur.builder()
                .code(dto.code())
                .residence(residence)
                .rfidTag(dto.rfidTag())
                .actif(true)
                .build();

        return toResponseDto(conteneurRepository.save(conteneur));
    }

    @Transactional
    public ConteneurResponseDto update(Long id, ConteneurRequestDto dto) {

        Conteneur conteneur = conteneurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteneur introuvable"));

        if (!conteneur.getResidence().getId().equals(dto.residenceId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un conteneur ne peut pas changer de résidence");
        }

        if (!conteneur.getCode().equals(dto.code())
                && conteneurRepository.existsByResidenceIdAndCode(dto.residenceId(), dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un conteneur avec ce code existe déjà pour cette résidence");
        }

        conteneur.setCode(dto.code());
        conteneur.setRfidTag(dto.rfidTag());

        return toResponseDto(conteneurRepository.save(conteneur));
    }

    public ConteneurResponseDto getById(Long id) {
        Conteneur conteneur = conteneurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteneur introuvable"));
        return toResponseDto(conteneur);
    }

    public List<ConteneurResponseDto> getByResidence(Long residenceId) {
        return conteneurRepository.findByResidenceId(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id) {
        Conteneur conteneur = conteneurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteneur introuvable"));

        if (arretConteneurRepository.existsActifByConteneurId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de désactiver : ce conteneur a un arrêt en attente sur une tournée active");
        }

        conteneur.setActif(false);
        conteneurRepository.save(conteneur);
    }

    private ConteneurResponseDto toResponseDto(Conteneur c) {
        return new ConteneurResponseDto(
                c.getId(),
                c.getCode(),
                c.getResidence().getId(),
                c.getResidence().getNom(),
                c.getRfidTag(),
                c.getActif()
        );
    }
}
