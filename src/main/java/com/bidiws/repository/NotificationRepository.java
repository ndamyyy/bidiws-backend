package com.bidiws.repository;

import com.bidiws.entity.Notification;
import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.TypeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataireId(Long destinataireId);

    List<Notification> findByDestinataireIdOrderByCreatedAtDesc(Long destinataireId);

    List<Notification> findByDestinataireIdAndLuFalse(Long residenceId);

    long countByDestinataireIdAndLuFalse(Long residenceId);

    List<Notification> findByArretId(Long arretId);

    List<Notification> findByEnvoyeFalse();

    List<Notification> findByType(TypeNotification type);

    List<Notification> findByCanal(CanalNotification canal);

}
