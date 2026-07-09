package com.passeportreparation.repairer.entity;

import com.passeportreparation.common.enums.ApplianceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "repairers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repairer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String phone;
    private String email;
    private String whatsapp;

    private Double latitude;
    private Double longitude;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "repairer_categories", joinColumns = @JoinColumn(name = "repairer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    @Builder.Default
    private Set<ApplianceCategory> categories = new HashSet<>();

    private boolean active;
}
