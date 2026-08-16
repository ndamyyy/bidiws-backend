package com.bidiws.service;

import com.bidiws.dto.appareiliot.AppareilIotCreeResponseDto;
import com.bidiws.dto.appareiliot.AppareilIotRequestDto;
import com.bidiws.dto.appareiliot.AppareilIotResponseDto;
import com.bidiws.entity.AppareilIot;
import com.bidiws.entity.Camion;
import com.bidiws.entity.Conteneur;
import com.bidiws.repository.AppareilIotRepository;
import com.bidiws.repository.CamionRepository;
import com.bidiws.repository.ConteneurRepository;
import com.bidiws.security.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppareilIotService {

    private final AppareilIotRepository appareilIotRepository;
    private final ConteneurRepository conteneurRepository;
    private final CamionRepository camionRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Transactional
    public AppareilIotCreeResponseDto create(AppareilIotRequestDto dto) {

        if (appareilIotRepository.existsByIdentifiantMateriel(dto.identifiantMateriel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un appareil avec cet identifiant matériel est déjà enregistré");
        }

        AppareilIot appareil = AppareilIot.builder()
                .identifiantMateriel(dto.identifiantMateriel())
                .typeAppareil(dto.typeAppareil())
                .actif(true)
                .build();

        appliquerRattachement(appareil, dto.conteneurId(), dto.camionId());

        String cleEnClair = apiKeyHasher.genererCle();
        appareil.setCleApiHash(apiKeyHasher.hash(cleEnClair));

        AppareilIot saved = appareilIotRepository.save(appareil);

        return new AppareilIotCreeResponseDto(saved.getId(), saved.getIdentifiantMateriel(), cleEnClair);
    }

    @Transactional
    public AppareilIotResponseDto update(Long id, AppareilIotRequestDto dto) {

        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));

        if (!appareil.getIdentifiantMateriel().equals(dto.identifiantMateriel())
                && appareilIotRepository.existsByIdentifiantMateriel(dto.identifiantMateriel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un appareil avec cet identifiant matériel est déjà enregistré");
        }

        appareil.setIdentifiantMateriel(dto.identifiantMateriel());
        appareil.setTypeAppareil(dto.typeAppareil());
        appliquerRattachement(appareil, dto.conteneurId(), dto.camionId());

        return toResponseDto(appareilIotRepository.save(appareil));
    }

    // Un appareil ne peut pas etre rattache a la fois a un conteneur et a un
    // camion (contrainte chk_appareil_iot_un_seul_rattachement en base) :
    // verifie ici aussi pour renvoyer un message clair plutot qu'une
    // erreur SQL brute.
    private void appliquerRattachement(AppareilIot appareil, Long conteneurId, Long camionId) {

        if (conteneurId != null && camionId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un appareil ne peut pas être rattaché à la fois à un conteneur et à un camion");
        }

        if (conteneurId != null) {
            Conteneur conteneur = conteneurRepository.findById(conteneurId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteneur introuvable"));
            if (!Boolean.TRUE.equals(conteneur.getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Impossible de rattacher un appareil à un conteneur inactif");
            }
            appareil.setConteneur(conteneur);
            appareil.setCamion(null);
        } else if (camionId != null) {
            Camion camion = camionRepository.findById(camionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));
            if (!Boolean.TRUE.equals(camion.getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Impossible de rattacher un appareil à un camion inactif");
            }
            appareil.setCamion(camion);
            appareil.setConteneur(null);
        } else {
            appareil.setConteneur(null);
            appareil.setCamion(null);
        }
    }

    public AppareilIotResponseDto getById(Long id) {
        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));
        return toResponseDto(appareil);
    }

    public List<AppareilIotResponseDto> getAll() {
        return appareilIotRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void desactiver(Long id) {
        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));
        appareil.setActif(false);
        appareilIotRepository.save(appareil);
    }

    // Invalide la cle actuelle et en genere une nouvelle (rotation) : utile
    // si une cle a fuite, sans avoir a recreer tout l'enregistrement.
    @Transactional
    public AppareilIotCreeResponseDto regenererCle(Long id) {
        AppareilIot appareil = appareilIotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appareil introuvable"));

        String cleEnClair = apiKeyHasher.genererCle();
        appareil.setCleApiHash(apiKeyHasher.hash(cleEnClair));
        appareilIotRepository.save(appareil);

        return new AppareilIotCreeResponseDto(appareil.getId(), appareil.getIdentifiantMateriel(), cleEnClair);
    }

    private AppareilIotResponseDto toResponseDto(AppareilIot a) {
        return new AppareilIotResponseDto(
                a.getId(),
                a.getIdentifiantMateriel(),
                a.getTypeAppareil(),
                a.getConteneur() != null ? a.getConteneur().getId() : null,
                a.getConteneur() != null ? a.getConteneur().getCode() : null,
                a.getCamion() != null ? a.getCamion().getId() : null,
                a.getCamion() != null ? a.getCamion().getImmatriculation() : null,
                a.getActif(),
                a.getCreatedAt()
        );
    }
}
