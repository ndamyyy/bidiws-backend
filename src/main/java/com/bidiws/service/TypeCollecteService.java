package com.bidiws.service;

import com.bidiws.dto.typecollecte.TypeCollecteRequestDto;
import com.bidiws.dto.typecollecte.TypeCollecteResponseDto;
import com.bidiws.entity.TypeCollecte;
import com.bidiws.repository.CalendrierCollecteRepository;
import com.bidiws.repository.TourneeRepository;
import com.bidiws.repository.TypeCollecteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeCollecteService {

    private final TypeCollecteRepository typeCollecteRepository;
    private final CalendrierCollecteRepository calendrierCollecteRepository;
    private final TourneeRepository tourneeRepository;

    @Transactional
    public TypeCollecteResponseDto create(TypeCollecteRequestDto dto) {

        if (typeCollecteRepository.existsByCode(dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code de type de collecte existe déjà");
        }

        TypeCollecte typeCollecte = TypeCollecte.builder()
                .code(dto.code())
                .libelle(dto.libelle())
                .couleur(dto.couleur())
                .icone(dto.icone())
                .build();

        return toResponseDto(typeCollecteRepository.save(typeCollecte));
    }

    @Transactional
    public TypeCollecteResponseDto update(Long id, TypeCollecteRequestDto dto) {

        TypeCollecte typeCollecte = typeCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));

        if (!typeCollecte.getCode().equals(dto.code()) && typeCollecteRepository.existsByCode(dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code de type de collecte existe déjà");
        }

        typeCollecte.setCode(dto.code());
        typeCollecte.setLibelle(dto.libelle());
        typeCollecte.setCouleur(dto.couleur());
        typeCollecte.setIcone(dto.icone());

        return toResponseDto(typeCollecteRepository.save(typeCollecte));
    }

    public TypeCollecteResponseDto getById(Long id) {
        TypeCollecte typeCollecte = typeCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));
        return toResponseDto(typeCollecte);
    }

    public List<TypeCollecteResponseDto> getAll() {
        return typeCollecteRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        TypeCollecte typeCollecte = typeCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de collecte introuvable"));

        if (!calendrierCollecteRepository.findByTypeCollecteId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : ce type de collecte est utilisé dans un calendrier");
        }

        if (tourneeRepository.existsByTypeCollecteId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer : ce type de collecte est utilisé par une tournée");
        }

        typeCollecteRepository.deleteById(typeCollecte.getId());
    }

    private TypeCollecteResponseDto toResponseDto(TypeCollecte t) {
        return new TypeCollecteResponseDto(
                t.getId(),
                t.getCode(),
                t.getLibelle(),
                t.getCouleur(),
                t.getIcone()
        );
    }
}