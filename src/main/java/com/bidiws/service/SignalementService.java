package com.bidiws.service;

import com.bidiws.dto.signalement.SignalementRequestDto;
import com.bidiws.dto.signalement.SignalementResponseDto;
import com.bidiws.entity.Arret;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Signalement;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.StatutSignalement;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.SignalementRepository;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignalementService {

    private final SignalementRepository signalementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResidenceRepository residenceRepository;
    private final ArretRepository arretRepository;

    @Transactional
    public SignalementResponseDto create(Long auteurId, SignalementRequestDto dto) {

        Utilisateur auteur = utilisateurRepository.findById(auteurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auteur introuvable"));

        Residence residence = resoudreResidence(dto.residenceId());
        Arret arret = resoudreArret(dto.arretId());

        Signalement signalement = Signalement.builder()
                .auteur(auteur)
                .residence(residence)
                .arret(arret)
                .type(dto.type())
                .description(dto.description())
                .photoUrl(dto.photoUrl())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .statut(StatutSignalement.OUVERT)
                .build();

        return toResponseDto(signalementRepository.save(signalement));
    }

    @Transactional
    public SignalementResponseDto changerStatut(Long id, StatutSignalement nouveauStatut) {

        Signalement signalement = signalementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));

        signalement.setStatut(nouveauStatut);

        if (nouveauStatut == StatutSignalement.RESOLU) {
            signalement.setResoluAt(LocalDateTime.now());
        }

        return toResponseDto(signalementRepository.save(signalement));
    }

    public SignalementResponseDto getById(Long id) {
        Signalement signalement = signalementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));
        return toResponseDto(signalement);
    }

    public List<SignalementResponseDto> getByAuteur(Long auteurId) {
        return signalementRepository.findByAuteurIdOrderByCreatedAtDesc(auteurId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<SignalementResponseDto> getByStatut(StatutSignalement statut) {
        return signalementRepository.findByStatutOrderByCreatedAtAsc(statut).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private Residence resoudreResidence(Long residenceId) {
        if (residenceId == null) {
            return null;
        }
        return residenceRepository.findById(residenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));
    }

    private Arret resoudreArret(Long arretId) {
        if (arretId == null) {
            return null;
        }
        return arretRepository.findById(arretId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));
    }

    private SignalementResponseDto toResponseDto(Signalement s) {
        return new SignalementResponseDto(
                s.getId(),
                s.getAuteur().getId(),
                s.getAuteur().getNom(),
                s.getAuteur().getPrenom(),
                s.getResidence() != null ? s.getResidence().getId() : null,
                s.getResidence() != null ? s.getResidence().getNom() : null,
                s.getArret() != null ? s.getArret().getId() : null,
                s.getType(),
                s.getDescription(),
                s.getPhotoUrl(),
                s.getLatitude(),
                s.getLongitude(),
                s.getStatut(),
                s.getCreatedAt(),
                s.getResoluAt()
        );
    }
}