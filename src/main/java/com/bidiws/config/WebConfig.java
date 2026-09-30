package com.bidiws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

// Sert les fichiers uploades (bidiws.upload.dir) en statique sous /files/**.
// Route ajoutee aux PUBLIC_ROUTES de SecurityConfig : la lecture d'une
// image doit rester accessible via une simple balise <img src>, sans
// pouvoir attacher un header Authorization — seul l'upload (POST
// /uploads) reste authentifie.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${bidiws.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + new File(uploadDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }
}
