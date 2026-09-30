package com.bidiws.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var files = Files.list(tempDir)) {
            files.forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void stockeUneImageEtRenvoieUnNomUuidAvecLaBonneExtension() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String nom = fileStorageService.stocker(file);

        assertThat(nom).endsWith(".png");
        assertThat(nom).doesNotContain("photo"); // pas le nom d'origine
        assertThat(Files.exists(tempDir.resolve(nom))).isTrue();
        assertThat(Files.readAllBytes(tempDir.resolve(nom))).containsExactly(1, 2, 3);
    }

    @Test
    void refuseUnFichierVide() {
        MultipartFile vide = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.stocker(vide))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun fichier");
    }

    @Test
    void refuseUnTypeNonImage() {
        MultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileStorageService.stocker(pdf))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("images uniquement");
    }

    @Test
    void refuseUnFichierDePlusDe5Mo() {
        byte[] gros = new byte[5 * 1024 * 1024 + 1];
        MultipartFile file = new MockMultipartFile("file", "big.png", "image/png", gros);

        assertThatThrownBy(() -> fileStorageService.stocker(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("trop volumineux");
    }
}
