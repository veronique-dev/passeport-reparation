package com.passeportreparation.auth.service;

import com.passeportreparation.auth.entity.EmailToken;
import com.passeportreparation.auth.entity.EmailTokenType;
import com.passeportreparation.auth.entity.RefreshToken;
import com.passeportreparation.auth.entity.UserAccount;
import com.passeportreparation.auth.repository.EmailTokenRepository;
import com.passeportreparation.auth.repository.RefreshTokenRepository;
import com.passeportreparation.auth.repository.UserAccountRepository;
import com.passeportreparation.auth.security.JwtService;
import com.passeportreparation.common.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository users;
    private final EmailTokenRepository emailTokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMailService mailService;

    @Value("${app.jwt.refresh-ttl-days}")
    private long refreshTtlDays;

    @Value("${app.tokens.confirm-ttl-hours}")
    private long confirmTtlHours;

    @Value("${app.tokens.reset-ttl-hours}")
    private long resetTtlHours;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new AuthException("Un compte existe déjà avec cet email.");
        }

        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(blankToNull(request.getFirstName()))
                .emailVerified(false)
                .build();
        users.save(user);

        String rawToken = issueEmailToken(user.getId(), EmailTokenType.CONFIRM, confirmTtlHours);
        mailService.sendConfirmEmail(email, rawToken);

        return MessageResponse.builder()
                .message("Compte créé. Vérifie ta boîte mail pour confirmer ton adresse.")
                .build();
    }

    @Transactional
    public MessageResponse confirmEmail(String rawToken) {
        EmailToken token = requireUsableEmailToken(rawToken, EmailTokenType.CONFIRM);
        UserAccount user = users.findById(token.getUserId())
                .orElseThrow(() -> new AuthException("Compte introuvable."));
        user.setEmailVerified(true);
        token.setUsedAt(Instant.now());
        return MessageResponse.builder().message("Email confirmé. Tu peux te connecter.").build();
    }

    @Transactional
    public AuthTokensResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        UserAccount user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("Email ou mot de passe incorrect."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Email ou mot de passe incorrect.");
        }
        if (!user.isEmailVerified()) {
            throw new AuthException("Confirme ton email avant de te connecter.");
        }
        return issueSession(user);
    }

    @Transactional
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        String hash = hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Session expirée. Reconnecte-toi."));
        if (!stored.isActive()) {
            throw new AuthException("Session expirée. Reconnecte-toi.");
        }
        stored.setRevokedAt(Instant.now());
        UserAccount user = users.findById(stored.getUserId())
                .orElseThrow(() -> new AuthException("Compte introuvable."));
        return issueSession(user);
    }

    @Transactional
    public MessageResponse logout(RefreshTokenRequest request) {
        refreshTokens.findByTokenHash(hashToken(request.getRefreshToken())).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
        return MessageResponse.builder().message("Déconnecté.").build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse me(UUID userId) {
        return toProfile(requireUser(userId));
    }

    @Transactional
    public UserProfileResponse updateMe(UUID userId, UpdateProfileRequest request) {
        UserAccount user = requireUser(userId);
        if (request.getFirstName() != null) {
            user.setFirstName(blankToNull(request.getFirstName()));
        }
        return toProfile(user);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        users.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = issueEmailToken(user.getId(), EmailTokenType.RESET, resetTtlHours);
            mailService.sendResetEmail(user.getEmail(), rawToken);
        });
        return MessageResponse.builder()
                .message("Si un compte existe pour cet email, un lien de réinitialisation a été envoyé.")
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        EmailToken token = requireUsableEmailToken(request.getToken(), EmailTokenType.RESET);
        UserAccount user = requireUser(token.getUserId());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        token.setUsedAt(Instant.now());
        return MessageResponse.builder().message("Mot de passe mis à jour. Tu peux te connecter.").build();
    }

    private AuthTokensResponse issueSession(UserAccount user) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail());
        String rawRefresh = randomToken();
        RefreshToken refresh = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(rawRefresh))
                .expiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS))
                .build();
        refreshTokens.save(refresh);

        return AuthTokensResponse.builder()
                .accessToken(access)
                .refreshToken(rawRefresh)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.accessTtlSeconds())
                .user(toProfile(user))
                .build();
    }

    private String issueEmailToken(UUID userId, EmailTokenType type, long ttlHours) {
        String raw = randomToken();
        EmailToken token = EmailToken.builder()
                .userId(userId)
                .type(type)
                .tokenHash(hashToken(raw))
                .expiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS))
                .build();
        emailTokens.save(token);
        return raw;
    }

    private EmailToken requireUsableEmailToken(String rawToken, EmailTokenType type) {
        EmailToken token = emailTokens.findByTokenHashAndType(hashToken(rawToken), type)
                .orElseThrow(() -> new AuthException("Lien invalide ou expiré."));
        if (!token.isUsable()) {
            throw new AuthException("Lien invalide ou expiré.");
        }
        return token;
    }

    private UserAccount requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new AuthException("Compte introuvable."));
    }

    private static UserProfileResponse toProfile(UserAccount user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
