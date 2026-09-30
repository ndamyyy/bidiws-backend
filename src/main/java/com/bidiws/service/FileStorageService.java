package com.bidiws.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final List<String> TYPES_AUTORISES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${bidiws.upload.dir:uploads}")
    private String uploadDir;

    // Stocke le fichier sous un nom genere (UUID) et renvoie ce nom — pas
    // le nom d'origine, pour eviter collisions et traversee de chemin
    // (ex. "../../etc/passwd.jpg").
    public String stocker(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier envoye");
        }

        String contentType = file.getContentType();
        if (contentType == null || !TYPES_AUTORISES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de fichier non autorise — images uniquement (jpeg, png, webp, gif)");
        }

        String extension = extensionPour(contentType);
        String nomFichier = UUID.randomUUID() + extension;

        try {
            Path dossier = Path.of(uploadDir).toAbsolutePath();
            Files.createDirectories(dossier);

            Path destination = dossier.resolve(nomFichier);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'enregistrement du fichier");
        }

        return nomFichier;
    }

    private String extensionPour(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}
