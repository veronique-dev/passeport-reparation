package com.passeportreparation.diagnosis.repository;

import com.passeportreparation.diagnosis.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    List<Diagnosis> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
