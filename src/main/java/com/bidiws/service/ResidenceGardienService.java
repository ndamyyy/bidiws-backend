package com.bidiws.service;

import com.bidiws.dto.residencegardien.ResidenceGardienRequestDto;
import com.bidiws.dto.residencegardien.ResidenceGardienResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.ResidenceGardien;
import com.bidiws.entity.ResidenceGardienId;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.ResidenceGardienRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResidenceGardienService {

    private final ResidenceGardienRepository residenceGardienRepository;
    private final ResidenceRepository residenceRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public ResidenceGardienResponseDto affecter(ResidenceGardienRequestDto dto) {

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        Utilisateur gardien = utilisateurRepository.findById(dto.gardienId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gardien introuvable"));

        if (gardien.getRole() != Role.GARDIEN || !Boolean.TRUE.equals(gardien.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utilisateur sélectionné n'est pas un gardien actif");
        }

        if (dto.principal()) {
            residenceGardienRepository.findByResidenceIdAndPrincipalTrue(dto.residenceId())
                    .ifPresent(existant -> {
                        existant.setPrincipal(false);
                        residenceGardienRepository.save(existant);
                    });
        }

        ResidenceGardienId id = new ResidenceGardienId(dto.residenceId(), dto.gardienId());

        ResidenceGardien residenceGardien = ResidenceGardien.builder()
                .residenceGardienId(id)
                .residence(residence)
                .gardien(gardien)
                .principal(dto.principal())
                .build();

        return toResponseDto(residenceGardienRepository.save(residenceGardien));
    }

    @Transactional
    public void retirer(Long residenceId, Long gardienId) {
        residenceGardienRepository.deleteByResidenceIdAndGardienId(residenceId, gardienId);
    }

    public List<ResidenceGardienResponseDto> getByResidence(Long residenceId) {
        return residenceGardienRepository.findByResidenceId(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ResidenceGardienResponseDto> getByGardien(Long gardienId) {
        return residenceGardienRepository.findByGardienId(gardienId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private ResidenceGardienResponseDto toResponseDto(ResidenceGardien rg) {
        return new ResidenceGardienResponseDto(
                rg.getResidence().getId(),
                rg.getResidence().getNom(),
                rg.getGardien().getId(),
                rg.getGardien().getNom(),
                rg.getGardien().getPrenom(),
                rg.getPrincipal()
        );
    }
}
