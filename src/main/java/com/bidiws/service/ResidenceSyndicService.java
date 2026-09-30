package com.bidiws.service;

import com.bidiws.dto.residencesyndic.ResidenceSyndicRequestDto;
import com.bidiws.dto.residencesyndic.ResidenceSyndicResponseDto;
import com.bidiws.entity.Residence;
import com.bidiws.entity.ResidenceSyndic;
import com.bidiws.entity.ResidenceSyndicId;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.ResidenceSyndicRepository;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResidenceSyndicService {

    private final ResidenceSyndicRepository residenceSyndicRepository;
    private final ResidenceRepository residenceRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public ResidenceSyndicResponseDto affecter(ResidenceSyndicRequestDto dto) {

        Residence residence = residenceRepository.findById(dto.residenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));

        if (!Boolean.TRUE.equals(residence.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette résidence est désactivée");
        }

        Utilisateur syndic = utilisateurRepository.findById(dto.syndicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Syndic introuvable"));

        if (syndic.getRole() != Role.SYNDIC || !Boolean.TRUE.equals(syndic.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utilisateur sélectionné n'est pas un syndic actif");
        }

        ResidenceSyndicId id = new ResidenceSyndicId(dto.residenceId(), dto.syndicId());

        ResidenceSyndic residenceSyndic = ResidenceSyndic.builder()
                .residenceSyndicId(id)
                .residence(residence)
                .syndic(syndic)
                .build();

        return toResponseDto(residenceSyndicRepository.save(residenceSyndic));
    }

    @Transactional
    public void retirer(Long residenceId, Long syndicId) {
        residenceSyndicRepository.deleteByResidenceIdAndSyndicId(residenceId, syndicId);
    }

    public List<ResidenceSyndicResponseDto> getByResidence(Long residenceId) {
        return residenceSyndicRepository.findByResidenceId(residenceId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ResidenceSyndicResponseDto> getBySyndic(Long syndicId) {
        return residenceSyndicRepository.findBySyndicId(syndicId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private ResidenceSyndicResponseDto toResponseDto(ResidenceSyndic rs) {
        return new ResidenceSyndicResponseDto(
                rs.getResidence().getId(),
                rs.getResidence().getNom(),
                rs.getSyndic().getId(),
                rs.getSyndic().getNom(),
                rs.getSyndic().getPrenom()
        );
    }
}
