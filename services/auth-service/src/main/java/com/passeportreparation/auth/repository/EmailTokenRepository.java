package com.passeportreparation.auth.repository;

import com.passeportreparation.auth.entity.EmailToken;
import com.passeportreparation.auth.entity.EmailTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {
    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);
}
