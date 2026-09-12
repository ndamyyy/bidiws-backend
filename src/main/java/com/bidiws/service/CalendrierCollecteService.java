package com.bidiws.service;

import com.bidiws.dto.calendriercollecte.CalendrierCollecteRequestDto;
import com.bidiws.dto.calendriercollecte.CalendrierCollecteResponseDto;
import com.bidiws.dto.calendriercollecte.ResidenceADesservirDto;
import com.bidiws.entity.CalendrierCollecte;
import com.bidiws.entity.Residence;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.TypeCollecteRepository;
import com.bidiws.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendrierCollecteService {

    private final CalendrierCollecteRepository calendrierCollecteRepository;
    private final ResidenceRepository residenceRepository;
    private final TypeCollecteRepository typeCollecteRepository;
    private final ZoneRepository zoneRepository;

    @Transactional
    public CalendrierCollecteResponseDto create(CalendrierCollecteRequestDto dto) {

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        if (!Boolean.TRUE.equals(residence.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette résidence est désactivée");
        }

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

    // Résidences à desservir un jour donné, dans une zone donnée, pour un
    // type de collecte donné — déduit du calendrier récurrent. Lecture
    // seule : ne crée aucun arrêt ni aucune tournée, juste une suggestion
    // que l'admin valide (ou pas) côté frontend avant création groupée.
    //
    // Filtrage fait en Java plutôt que dans la requête (findByTypeCollecteId
    // existe déjà, sans condition supplémentaire) : plus facile à couvrir par
    // des tests unitaires Mockito, cohérent avec le reste du service qui
    // valide en Java plutôt que dans des requêtes derivées complexes.
    public List<ResidenceADesservirDto> getResidencesADesservir(Long zoneId, LocalDate date, Long typeCollecteId) {

        zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));

        typeCollecteRepository.findById(typeCollecteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));

        // jourSemaine : 1=lundi...7=dimanche (ISO-8601, DayOfWeek.getValue()),
        // meme convention que celle documentee sur CalendrierCollecte.jourSemaine.
        short jourSemaine = (short) date.getDayOfWeek().getValue();

        return calendrierCollecteRepository.findByTypeCollecteId(typeCollecteId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getActif()))
                .filter(c -> c.getJourSemaine() != null && c.getJourSemaine() == jourSemaine)
                .filter(c -> c.getResidence() != null
                        && Boolean.TRUE.equals(c.getResidence().getActif())
                        && c.getResidence().getZone() != null
                        && zoneId.equals(c.getResidence().getZone().getId()))
                .map(CalendrierCollecte::getResidence)
                .distinct()
                .map(this::toResidenceADesservirDto)
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

    private ResidenceADesservirDto toResidenceADesservirDto(Residence r) {
        return new ResidenceADesservirDto(
                r.getId(),
                r.getNom(),
                r.getAdresse(),
                r.getCodePostal(),
                r.getNbConteneurs()
        );
    }
}