package com.passeportreparation.diagnosis.entity;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.RepairVerdict;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String mediaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplianceCategory category;

    private String applianceLabel;
    private String probableIssue;
    private double confidence;

    private BigDecimal repairLow;
    private BigDecimal repairHigh;
    private BigDecimal replacementApprox;

    @Enumerated(EnumType.STRING)
    private RepairVerdict verdict;

    private boolean supported;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
