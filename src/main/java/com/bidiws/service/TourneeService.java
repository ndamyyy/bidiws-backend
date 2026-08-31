package com.bidiws.service;

import com.bidiws.dto.tournee.TourneeRequestDto;
import com.bidiws.dto.tournee.TourneeResponseDto;
import com.bidiws.entity.*;
import com.bidiws.enums.StatutTournee;
import com.bidiws.enums.Role;
import com.bidiws.repository.*;
import com.bidiws.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourneeService {

    private final TourneeRepository tourneeRepository;
    private final TypeCollecteRepository typeCollecteRepository;
    private final CamionRepository camionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ZoneRepository zoneRepository;
    private final ArretRepository arretRepository;
    private final ResidenceSyndicRepository residenceSyndicRepository;

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

        Long villeCamionId = camion.getVille() != null ? camion.getVille().getId() : null;
        Zone zone = resoudreZone(dto.zoneId(), villeCamionId);

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

    public List<TourneeResponseDto> getByDate(LocalDate date, CustomUserDetails userDetails) {
        return filtrerParRole(tourneeRepository.findByDateTournee(date), userDetails).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TourneeResponseDto> getByChauffeur(Long chauffeurId, CustomUserDetails userDetails) {
        return filtrerParRole(tourneeRepository.findByChauffeurId(chauffeurId), userDetails).stream()
                .map(this::toResponseDto)
                .toList();
    }

    // Combine les deux filtres explicites (date ET chauffeur) plutot que de les
    // traiter en either/or cote controller : un appel avec les deux ensemble
    // doit vraiment restreindre sur les deux niveaux, pas juste sur l'un des
    // deux au hasard de l'ordre des if.
    public List<TourneeResponseDto> getByDateAndChauffeur(LocalDate date, Long chauffeurId, CustomUserDetails userDetails) {
        return filtrerParRole(tourneeRepository.findByChauffeurIdAndDateTournee(chauffeurId, date), userDetails).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TourneeResponseDto> getAll(CustomUserDetails userDetails) {
        return filtrerParRole(tourneeRepository.findAll(), userDetails).stream()
                .map(this::toResponseDto)
                .toList();
    }

    // MAIRIE : fail-closed si pas de ville rattachee, une tournee sans zone n'a
    // pas de ville determinable donc reste invisible. Filtre sur zone.getVille(),
    // pas camion.getVille() : resoudreZone() garantit desormais camion.ville ==
    // zone.ville a la creation (quand les deux sont renseignes) — ne pas changer
    // ce critere sans revalider ce raisonnement.
    //
    // CHAUFFEUR : ne voit que ses propres tournees. Sans ce filtre, un chauffeur
    // (ou n'importe quel role passant le @PreAuthorize du controller) voyait
    // TOUTES les tournees de TOUTES les villes/chauffeurs des lors qu'il n'etait
    // pas MAIRIE — fuite confirmee, corrigee ici.
    //
    // ADMIN et tout autre role non liste ci-dessous (deja passe le @PreAuthorize
    // cote controller) : non restreint.
    private List<Tournee> filtrerParRole(List<Tournee> tournees, CustomUserDetails userDetails) {
        if (userDetails.getRole() == Role.MAIRIE) {
            if (userDetails.getVilleId() == null) {
                return List.of();
            }
            return tournees.stream()
                    .filter(t -> t.getZone() != null && t.getZone().getVille() != null
                            && t.getZone().getVille().getId().equals(userDetails.getVilleId()))
                    .toList();
        }
        if (userDetails.getRole() == Role.CHAUFFEUR) {
            return tournees.stream()
                    .filter(t -> t.getChauffeur() != null
                            && t.getChauffeur().getId().equals(userDetails.getId()))
                    .toList();
        }
        // SYNDIC : aucun lien direct tournee -> syndic, uniquement via les
        // arrets qui desservent une residence rattachee a ce syndic (meme
        // raisonnement que AuthorizationService.canAccessTournee, applique ici
        // a une liste entiere plutot qu'a une tournee unique). Fail-closed :
        // syndic sans residence rattachee -> aucune tournee visible.
        if (userDetails.getRole() == Role.SYNDIC) {
            List<Long> residenceIds = residenceSyndicRepository.findBySyndicId(userDetails.getId()).stream()
                    .map(rs -> rs.getResidence().getId())
                    .toList();
            if (residenceIds.isEmpty()) {
                return List.of();
            }
            Set<Long> tourneeIdsAccessibles = arretRepository.findByResidenceIdIn(residenceIds).stream()
                    .map(a -> a.getTournee().getId())
                    .collect(Collectors.toSet());
            return tournees.stream()
                    .filter(t -> tourneeIdsAccessibles.contains(t.getId()))
                    .toList();
        }
        return tournees;
    }

    // Meme pattern que ResidenceService.resoudreZone : rejette une zone qui
    // n'appartient pas a la ville du camion, plutot que de laisser passer une
    // incoherence qui fausserait ensuite filtrerParVille (fuite cross-tenant).
    //
    // villeCamionId peut etre null (Camion.ville est nullable, cf. migration V4) :
    // dans ce cas on laisse passer sans validation (fail-open), pas de cas
    // couvert ailleurs dans le projet pour un camion sans ville rattachee — a la
    // difference du scoping MAIRIE qui est fail-closed.
    private Zone resoudreZone(Long zoneId, Long villeCamionId) {
        if (zoneId == null) {
            return null;
        }
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));

        if (villeCamionId != null && !zone.getVille().getId().equals(villeCamionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette zone n'appartient pas à la ville du camion sélectionné");
        }

        return zone;
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
