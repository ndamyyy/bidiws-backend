package com.bidiws.entity;

import com.bidiws.enums.SourceGps;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signal_gps")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignalGps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournee_id", nullable = false)
    private Tournee tournee;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "vitesse_kmh", precision = 5, scale = 2)
    private BigDecimal vitesseKmh;

    @Column(name = "cap", precision = 5, scale = 2)
    private BigDecimal cap;

    @Column(name = "precision_m")
    private Short precisionM;

    @Column(name = "horodatage", nullable = false)
    private LocalDateTime horodatage;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    @Builder.Default
    private SourceGps source = SourceGps.APP;
}
