package com.bidiws.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bean FirebaseMessaging — cree UNIQUEMENT si bidiws.fcm.credentials-path
 * est renseigne (@ConditionalOnProperty, sans havingValue : absent du
 * tout => pas de bean, pas d'erreur au demarrage). Aucun projet Firebase
 * n'existe encore pour BIDIWS a ce jour : c'est l'etat attendu tant que
 * la propriete n'est pas configuree — voir FcmPushService, qui injecte
 * ce bean en Optional&lt;FirebaseMessaging&gt; et se degrade en no-op
 * (best-effort) plutot que de simuler un envoi reussi.
 *
 * Pour activer : creer un projet Firebase, telecharger la cle de compte
 * de service (Project Settings > Service accounts > Generate new private
 * key), et definir bidiws.fcm.credentials-path vers ce fichier JSON (hors
 * du depot, comme application.properties lui-meme).
 */
@Slf4j
@Configuration
public class FcmConfig {

    @Bean
    @ConditionalOnProperty(name = "bidiws.fcm.credentials-path")
    public FirebaseMessaging firebaseMessaging(org.springframework.core.env.Environment env) {
        String credentialsPath = env.getProperty("bidiws.fcm.credentials-path");

        try (InputStream in = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();

            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();

            log.info("FCM initialise avec succes depuis {}", credentialsPath);
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            // Config presente mais invalide (chemin errone, JSON corrompu...) :
            // signale clairement, mais ne bloque pas le demarrage de l'app —
            // meme philosophie best-effort que le reste de l'integration FCM.
            log.error("FCM : echec d'initialisation depuis bidiws.fcm.credentials-path={} — "
                    + "les notifications push FCM resteront desactivees", credentialsPath, e);
            return null;
        }
    }
}
