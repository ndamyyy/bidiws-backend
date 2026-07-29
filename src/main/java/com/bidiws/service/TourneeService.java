package com.bidiws.service;

import com.bidiws.dto.tournee.TourneeRequestDto;
import com.bidiws.dto.tournee.TourneeResponseDto;
import com.bidiws.entity.*;
import com.bidiws.enums.StatutTournee;
import com.bidiws.enums.Role;
import com.bidiws.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourneeService {

    private final TourneeRepository tourneeRepository;
    private final TypeCollecteRepository typeCollecteRepository;
    private final CamionRepository camionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ZoneRepository zoneRepository;

    @Transactional
    public TourneeResponseDto create(TourneeRequestDto dto) {

        TypeCollecte typeCollecte = typeCollecteRepository.findById(dto.typeCollecteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));

        Camion camion = camionRepository.findById(dto.camionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));

        Utilisateur chauffeur = utilisateurRepository.findById(dto.chauffeurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chauffeur introuvable"));

        if (chauffeur.getRole() != Role.CHAUFFEUR || !Boolean.TRUE.equals(chauffeur.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utilisateur sélectionné n'est pas un chauffeur actif");
        }

        if (!Boolean.TRUE.equals(camion.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le camion sélectionné est inactif");
        }

        Zone zone = resoudreZone(dto.zoneId());

        Tournee tournee = Tournee.builder()
                .dateTournee(dto.dateTournee())
                .typeCollecte(typeCollecte)
                .camion(camion)
                .chauffeur(chauffeur)
                .zone(zone)
                .statut(StatutTournee.PLANIFIEE)
                .build();

        return toResponseDto(tourneeRepository.save(tournee));
    }

    @Transactional
    public TourneeResponseDto demarrer(Long id) {

        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));

        if (tournee.getStatut() != StatutTournee.PLANIFIEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seule une tournée planifiée peut être démarrée");
        }

        tournee.setStatut(StatutTournee.EN_COURS);
        tournee.setHeureDebut(LocalDateTime.now());

        return toResponseDto(tourneeRepository.save(tournee));
    }

    @Transactional
    public TourneeResponseDto terminer(Long id) {

        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));

        if (tournee.getStatut() != StatutTournee.EN_COURS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seule une tournée en cours peut être terminée");
        }

        tournee.setStatut(StatutTournee.TERMINEE);
        tournee.setHeureFin(LocalDateTime.now());

        return toResponseDto(tourneeRepository.save(tournee));
    }

    @Transactional
    public TourneeResponseDto annuler(Long id) {

        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));

        if (tournee.getStatut() == StatutTournee.TERMINEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible d'annuler une tournée déjà terminée");
        }

        tournee.setStatut(StatutTournee.ANNULEE);

        return toResponseDto(tourneeRepository.save(tournee));
    }

    public TourneeResponseDto getById(Long id) {
        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));
        return toResponseDto(tournee);
    }

    public List<TourneeResponseDto> getByDate(LocalDate date) {
        return tourneeRepository.findByDateTournee(date).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TourneeResponseDto> getByChauffeur(Long chauffeurId) {
        return tourneeRepository.findByChauffeurId(chauffeurId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TourneeResponseDto> getAll() {
        return tourneeRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    private Zone resoudreZone(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));
    }

    private TourneeResponseDto toResponseDto(Tournee t) {
        return new TourneeResponseDto(
                t.getId(),
                t.getDateTournee(),
                t.getTypeCollecte().getId(),
                t.getTypeCollecte().getLibelle(),
                t.getCamion().getId(),
                t.getCamion().getImmatriculation(),
                t.getChauffeur().getId(),
                t.getChauffeur().getNom(),
                t.getChauffeur().getPrenom(),
                t.getZone() != null ? t.getZone().getId() : null,
                t.getZone() != null ? t.getZone().getNom() : null,
                t.getStatut(),
                t.getHeureDebut(),
                t.getHeureFin()
        );
    }
}
