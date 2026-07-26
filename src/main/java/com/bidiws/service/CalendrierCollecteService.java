package com.bidiws.service;

import com.bidiws.dto.calendriercollecte.CalendrierCollecteRequestDto;
import com.bidiws.dto.calendriercollecte.CalendrierCollecteResponseDto;
import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.Residence;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TypeCollecteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendrierCollecteService {

    private final CalendrierCollecteRepository calendrierCollecteRepository;
    private final ResidenceRepository residenceRepository;
    private final TypeCollecteRepository typeCollecteRepository;

    @Transactional
    public CalendrierCollecteResponseDto create(CalendrierCollecteRequestDto dto) {

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        TypeCollecte typeCollecte = typeCollecteRepository.findById(dto.typeCollecteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));

        CalendrierCollecte calendrier = CalendrierCollecte.builder()
                .residence(residence)
                .typeCollecte(typeCollecte)
                .jourSemaine(dto.jourSemaine().shortValue())
                .heureEstimee(dto.heureEstimee())
                .actif(true)
                .build();

        return toResponseDto(calendrierCollecteRepository.save(calendrier));
    }

    @Transactional
    public CalendrierCollecteResponseDto update(Long id, CalendrierCollecteRequestDto dto) {

        CalendrierCollecte calendrier = calendrierCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendrier introuvable"));

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        TypeCollecte typeCollecte = typeCollecteRepository.findById(dto.typeCollecteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));

        calendrier.setResidence(residence);
        calendrier.setTypeCollecte(typeCollecte);
        calendrier.setJourSemaine(dto.jourSemaine().shortValue());
        calendrier.setHeureEstimee(dto.heureEstimee());

        return toResponseDto(calendrierCollecteRepository.save(calendrier));
    }

    @Transactional
    public void desactiver(Long id) {
        CalendrierCollecte calendrier = calendrierCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendrier introuvable"));
        calendrier.setActif(false);
        calendrierCollecteRepository.save(calendrier);
    }

    public CalendrierCollecteResponseDto getById(Long id) {
        CalendrierCollecte calendrier = calendrierCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendrier introuvable"));
        return toResponseDto(calendrier);
    }

    public List<CalendrierCollecteResponseDto> getByResidence(Long residenceId) {
        return calendrierCollecteRepository.findByResidenceIdAndActifTrue(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private CalendrierCollecteResponseDto toResponseDto(CalendrierCollecte c) {
        return new CalendrierCollecteResponseDto(
                c.getId(),
                c.getResidence().getId(),
                c.getResidence().getNom(),
                c.getTypeCollecte().getId(),
                c.getTypeCollecte().getLibelle(),
                c.getJourSemaine().intValue(),
                c.getHeureEstimee(),
                c.getActif()
        );
    }
}