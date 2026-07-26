package com.bidiws.service;

import com.bidiws.dto.arret.ArretIncidentRequestDto;
import com.bidiws.dto.arret.ArretRequestDto;
import com.bidiws.dto.arret.ArretResponseDto;
import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Tournee;
import com.bidiws.enums.StatutArret;
import com.bidiws.event.ArretStatutChangeEvent;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArretService {

    private final ArretRepository arretRepository;
    private final TourneeRepository tourneeRepository;
    private final ResidenceRepository residenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ArretResponseDto creer(ArretRequestDto dto) {

        Tournee tournee = tourneeRepository.findById(dto.tourneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        Arret arret = Arret.builder()
                .tournee(tournee)
                .residence(residence)
                .ordre(dto.ordre().shortValue())
                .statut(StatutArret.EN_ATTENTE)
                .nbConteneurs(dto.nbConteneurs() != null ? dto.nbConteneurs().shortValue() : null)
                .typesConteneurs(dto.typesConteneurs())
                .build();

        return toResponseDto(arretRepository.save(arret));
    }

    @Transactional
    public ArretResponseDto changerStatut(Long id, StatutArret nouveauStatut) {

        Arret arret = arretRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));

        StatutArret ancienStatut = arret.getStatut();
        arret.setStatut(nouveauStatut);

        Arret saved = arretRepository.save(arret);

        eventPublisher.publishEvent(new ArretStatutChangeEvent(
                saved.getId(),
                saved.getTournee().getId(),
                saved.getResidence().getId(),
                ancienStatut,
                nouveauStatut
        ));

        return toResponseDto(saved);
    }

    @Transactional
    public ArretResponseDto signalerIncident(Long id, ArretIncidentRequestDto dto) {

        Arret arret = arretRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));

        StatutArret ancienStatut = arret.getStatut();

        arret.setIncident(true);
        arret.setDescriptionIncident(dto.descriptionIncident());
        arret.setPhotoIncidentUrl(dto.photoIncidentUrl());
        arret.setStatut(StatutArret.INCIDENT);

        Arret saved = arretRepository.save(arret);

        eventPublisher.publishEvent(new ArretStatutChangeEvent(
                saved.getId(),
                saved.getTournee().getId(),
                saved.getResidence().getId(),
                ancienStatut,
                StatutArret.INCIDENT
        ));

        return toResponseDto(saved);
    }

    public ArretResponseDto getById(Long id) {
        Arret arret = arretRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));
        return toResponseDto(arret);
    }

    public List<ArretResponseDto> getByTournee(Long tourneeId) {
        return arretRepository.findByTourneeIdOrderByOrdreAsc(tourneeId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ArretResponseDto> getByResidence(Long residenceId) {
        return arretRepository.findByResidenceIdOrderByCreatedAtDesc(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private ArretResponseDto toResponseDto(Arret a) {
        return new ArretResponseDto(
                a.getId(),
                a.getTournee().getId(),
                a.getResidence().getId(),
                a.getResidence().getNom(),
                a.getOrdre() != null ? a.getOrdre().intValue() : null,
                a.getStatut(),
                a.getHeureEstimee(),
                a.getHeureApproche(),
                a.getHeureCollecte(),
                a.getScoreConfiance() != null ? a.getScoreConfiance().intValue() : null,
                a.getModeDetection(),
                a.getNbConteneurs() != null ? a.getNbConteneurs().intValue() : null,
                a.getTypesConteneurs(),
                a.getPoidsKg(),
                a.getIncident(),
                a.getDescriptionIncident(),
                a.getPhotoIncidentUrl()
        );
    }
}