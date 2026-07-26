package com.bidiws.service;

import com.bidiws.dto.chauffeurcamion.ChauffeurCamionRequestDto;
import com.bidiws.dto.chauffeurcamion.ChauffeurCamionResponseDto;
import com.bidiws.entity.Camion;
import com.bidiws.entity.ChauffeurCamion;
import com.bidiws.entity.ChauffeurCamionId;
import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ChauffeurCamionRepository;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChauffeurCamionService {

    private final ChauffeurCamionRepository chauffeurCamionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CamionRepository camionRepository;

    @Transactional
    public ChauffeurCamionResponseDto affecter(ChauffeurCamionRequestDto dto) {

        Utilisateur chauffeur = utilisateurRepository.findById(dto.chauffeurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chauffeur introuvable"));

        Camion camion = camionRepository.findById(dto.camionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));

        if (chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(dto.camionId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce camion a déjà un chauffeur actuellement affecté");
        }

        if (chauffeurCamionRepository.findByChauffeurIdAndDateFinIsNull(dto.chauffeurId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce chauffeur conduit déjà un autre camion actuellement");
        }

        ChauffeurCamionId id = new ChauffeurCamionId(dto.chauffeurId(), dto.camionId(), dto.dateDebut());

        ChauffeurCamion affectation = ChauffeurCamion.builder()
                .chauffeurCamionId(id)
                .chauffeur(chauffeur)
                .camion(camion)
                .dateFin(dto.dateFin())
                .build();

        return toResponseDto(chauffeurCamionRepository.save(affectation));
    }

    @Transactional
    public ChauffeurCamionResponseDto terminer(Long chauffeurId, Long camionId) {

        ChauffeurCamion affectation = chauffeurCamionRepository.findByCamionIdAndDateFinIsNull(camionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune affectation active trouvée pour ce camion"));

        if (!affectation.getChauffeur().getId().equals(chauffeurId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce chauffeur n'est pas celui affecté à ce camion");
        }

        affectation.setDateFin(LocalDate.now());

        return toResponseDto(chauffeurCamionRepository.save(affectation));
    }

    public List<ChauffeurCamionResponseDto> getByChauffeur(Long chauffeurId) {
        return chauffeurCamionRepository.findByChauffeurId(chauffeurId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ChauffeurCamionResponseDto> getByCamion(Long camionId) {
        return chauffeurCamionRepository.findByCamionId(camionId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private ChauffeurCamionResponseDto toResponseDto(ChauffeurCamion cc) {
        return new ChauffeurCamionResponseDto(
                cc.getChauffeur().getId(),
                cc.getChauffeur().getNom(),
                cc.getChauffeur().getPrenom(),
                cc.getCamion().getId(),
                cc.getCamion().getImmatriculation(),
                cc.getDateDebut(),
                cc.getDateFin()
        );
    }
}