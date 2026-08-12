package com.bidiws.service;

import com.bidiws.dto.notification.NotificationRequestDto;
import com.bidiws.dto.notification.NotificationResponseDto;
import com.bidiws.entity.Arret;
import com.bidiws.entity.Notification;
import com.bidiws.entity.Residence;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.TypeNotification;
import com.bidiws.event.NotificationCreeeEvent;
import com.bidiws.repository.ArretRepository;
import com.bidiws.repository.NotificationRepository;
import com.bidiws.repository.ResidenceRepository;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ArretRepository arretRepository;
    private final ResidenceRepository residenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Notification creerNotification(
            Long destinataireId,
            Long arretId,
            Long residenceId,
            TypeNotification type,
            String titre,
            String message,
            CanalNotification canal
    ) {

        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable"));

        Arret arret = resoudreArret(arretId);
        Residence residence = resoudreResidence(residenceId);

        Notification notification = Notification.builder()
                .destinataire(destinataire)
                .arret(arret)
                .residence(residence)
                .type(type)
                .titre(titre)
                .message(message)
                .canal(canal != null ? canal : CanalNotification.PUSH)
                .lu(false)
                .envoye(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        eventPublisher.publishEvent(new NotificationCreeeEvent(
                saved.getId(),
                saved.getDestinataire().getId()
        ));

        return saved;
    }

    @Transactional
    public NotificationResponseDto create(NotificationRequestDto dto) {

        Notification notification = creerNotification(
                dto.destinataireId(),
                dto.arretId(),
                dto.residenceId(),
                dto.type(),
                dto.titre(),
                dto.message(),
                dto.canal()
        );

        return toResponseDto(notification);
    }

    @Transactional
    public NotificationResponseDto marquerLue(Long id, Long destinataireId) {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));

        if (!notification.getDestinataire().getId().equals(destinataireId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }

        notification.setLu(true);
        notification.setHeureLecture(LocalDateTime.now());

        return toResponseDto(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponseDto marquerEnvoyee(Long id) {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));

        notification.setEnvoye(true);
        notification.setHeureEnvoi(LocalDateTime.now());

        return toResponseDto(notificationRepository.save(notification));
    }

    public NotificationResponseDto getById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        return toResponseDto(notification);
    }

    public List<NotificationResponseDto> getByDestinataire(Long destinataireId) {
        return notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(destinataireId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<NotificationResponseDto> getNonLues(Long destinataireId) {
        return notificationRepository.findByDestinataireIdAndLuFalse(destinataireId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public long countNonLues(Long destinataireId) {
        return notificationRepository.countByDestinataireIdAndLuFalse(destinataireId);
    }

    private Arret resoudreArret(Long arretId) {
        if (arretId == null) {
            return null;
        }
        return arretRepository.findById(arretId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrêt introuvable"));
    }

    private Residence resoudreResidence(Long residenceId) {
        if (residenceId == null) {
            return null;
        }
        return residenceRepository.findById(residenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résidence introuvable"));
    }

    private NotificationResponseDto toResponseDto(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getDestinataire().getId(),
                n.getArret() != null ? n.getArret().getId() : null,
                n.getResidence() != null ? n.getResidence().getId() : null,
                n.getType(),
                n.getTitre(),
                n.getMessage(),
                n.getCanal(),
                n.getLu(),
                n.getEnvoye(),
                n.getHeureEnvoi(),
                n.getHeureLecture()
        );
    }
}
