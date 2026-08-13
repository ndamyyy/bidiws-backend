package com.bidiws.service;

import com.bidiws.dto.residencehabitant.ResidenceHabitantRequestDto;
import com.bidiws.dto.residencehabitant.ResidenceHabitantResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.ResidenceHabitant;
import com.bidiws.entity.ResidenceHabitantId;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.ResidenceHabitantRepository;
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
public class ResidenceHabitantService {

    private final ResidenceHabitantRepository residenceHabitantRepository;
    private final ResidenceRepository residenceRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public ResidenceHabitantResponseDto affecter(ResidenceHabitantRequestDto dto) {

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        Utilisateur habitant = utilisateurRepository.findById(dto.habitantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitant introuvable"));

        if (habitant.getRole() != Role.HABITANT || !Boolean.TRUE.equals(habitant.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utilisateur sélectionné n'est pas un habitant actif");
        }

        ResidenceHabitantId id = new ResidenceHabitantId(dto.residenceId(), dto.habitantId());

        ResidenceHabitant residenceHabitant = ResidenceHabitant.builder()
                .residenceHabitantId(id)
                .residence(residence)
                .habitant(habitant)
                .build();

        return toResponseDto(residenceHabitantRepository.save(residenceHabitant));
    }

    @Transactional
    public void retirer(Long residenceId, Long habitantId) {
        residenceHabitantRepository.deleteByResidenceIdAndHabitantId(residenceId, habitantId);
    }

    public List<ResidenceHabitantResponseDto> getByResidence(Long residenceId) {
        return residenceHabitantRepository.findByResidenceId(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ResidenceHabitantResponseDto> getByHabitant(Long habitantId) {
        return residenceHabitantRepository.findByHabitantId(habitantId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private ResidenceHabitantResponseDto toResponseDto(ResidenceHabitant rh) {
        return new ResidenceHabitantResponseDto(
                rh.getResidence().getId(),
                rh.getResidence().getNom(),
                rh.getHabitant().getId(),
                rh.getHabitant().getNom(),
                rh.getHabitant().getPrenom()
        );
    }
}
