package com.passeportreparation.repairer.repository;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.repairer.entity.Repairer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RepairerRepository extends JpaRepository<Repairer, UUID> {

    @Query("""
            SELECT DISTINCT r FROM Repairer r
            JOIN r.categories c
            WHERE r.active = true
              AND (:category IS NULL OR c = :category)
              AND (:city IS NULL OR LOWER(r.city) = LOWER(:city))
            """)
    List<Repairer> search(@Param("category") ApplianceCategory category, @Param("city") String city);
}
