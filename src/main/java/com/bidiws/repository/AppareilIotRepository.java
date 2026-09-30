package com.bidiws.repository;

import com.bidiws.entity.AppareilIot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppareilIotRepository extends JpaRepository<AppareilIot, Long> {

    // JOIN FETCH explicite : DeviceApiKeyAuthenticationFilter s'execute avant
    // l'ouverture de la session liee a la requete (open-in-view s'active a la
    // phase HandlerInterceptor, apres les filtres Spring Security). Sans ce
    // fetch, conteneur/camion restent des proxies detaches de la session du
    // filtre, et une LazyInitializationException surgit des qu'un champ non-id
    // (getActif(), getCode()...) est lu plus tard dans le service.
    @Query("SELECT a FROM AppareilIot a " +
            "LEFT JOIN FETCH a.conteneur c " +
            "LEFT JOIN FETCH c.residence " +
            "LEFT JOIN FETCH a.camion " +
            "WHERE a.cleApiHash = :cleApiHash")
    Optional<AppareilIot> findByCleApiHash(@Param("cleApiHash") String cleApiHash);

    boolean existsByIdentifiantMateriel(String identifiantMateriel);

    List<AppareilIot> findByConteneurResidenceId(Long residenceId);

    List<AppareilIot> findByCamionId(Long camionId);
}
