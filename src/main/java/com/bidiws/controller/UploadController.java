package com.bidiws.controller;

import com.bidiws.dto.upload.UploadResponseDto;
import com.bidiws.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// Utilitaire generique d'upload d'image — pas de restriction par role
// particuliere, ouvert a tout utilisateur authentifie (SecurityConfig,
// anyRequest().authenticated() couvre deja /uploads sans regle dediee).
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<UploadResponseDto> upload(@RequestParam("file") MultipartFile file) {

        String nomFichier = fileStorageService.stocker(file);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files/")
                .path(nomFichier)
                .toUriString();

        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponseDto(url));
    }
}
