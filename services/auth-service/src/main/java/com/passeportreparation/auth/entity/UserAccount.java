package com.passeportreparation.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 80)
    private String firstName;

    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        email = email == null ? null : email.trim().toLowerCase();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
