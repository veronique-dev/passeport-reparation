package com.passeportreparation.auth.service;

import com.passeportreparation.auth.entity.EmailToken;
import com.passeportreparation.auth.entity.EmailTokenType;
import com.passeportreparation.auth.entity.RefreshToken;
import com.passeportreparation.auth.entity.UserAccount;
import com.passeportreparation.auth.repository.EmailTokenRepository;
import com.passeportreparation.auth.repository.RefreshTokenRepository;
import com.passeportreparation.auth.repository.UserAccountRepository;
import com.passeportreparation.auth.security.JwtService;
import com.passeportreparation.common.dto.ForgotPasswordRequest;
import com.passeportreparation.common.dto.LoginRequest;
import com.passeportreparation.common.dto.RefreshTokenRequest;
import com.passeportreparation.common.dto.RegisterRequest;
import com.passeportreparation.common.dto.ResetPasswordRequest;
import com.passeportreparation.common.dto.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserAccountRepository users;
    @Mock EmailTokenRepository emailTokens;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock AuthMailService mailService;

    private AuthService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        JwtService jwt = new JwtService("passeport-reparation-dev-secret-change-me-32chars-min", 15);
        service = new AuthService(users, emailTokens, refreshTokens, encoder, jwt, mailService);
        ReflectionTestUtils.setField(service, "refreshTtlDays", 14L);
        ReflectionTestUtils.setField(service, "confirmTtlHours", 24L);
        ReflectionTestUtils.setField(service, "resetTtlHours", 1L);
    }

    @Test
    void registerCreatesUserAndSendsConfirmMail() {
        when(users.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(users.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(emailTokens.save(any(EmailToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.register(RegisterRequest.builder()
                .email("A@B.com")
                .password("secret123")
                .firstName("Ada")
                .build());

        assertThat(response.getMessage()).containsIgnoringCase("confirm");
        verify(mailService).sendConfirmEmail(eq("a@b.com"), any());
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(captor.getValue().isEmailVerified()).isFalse();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(RegisterRequest.builder()
                .email("a@b.com")
                .password("secret123")
                .build()))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void loginRequiresVerifiedEmail() {
        UserAccount user = UserAccount.builder()
                .id(UUID.randomUUID())
                .email("a@b.com")
                .passwordHash(encoder.encode("secret123"))
                .emailVerified(false)
                .build();
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(LoginRequest.builder()
                .email("a@b.com")
                .password("secret123")
                .build()))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Confirme");
    }

    @Test
    void loginRejectsBadPassword() {
        UserAccount user = UserAccount.builder()
                .id(UUID.randomUUID())
                .email("a@b.com")
                .passwordHash(encoder.encode("secret123"))
                .emailVerified(true)
                .build();
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(LoginRequest.builder()
                .email("a@b.com")
                .password("wrong-password")
                .build()))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void loginReturnsTokensWhenVerified() {
        UserAccount user = verifiedUser();
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var tokens = service.login(LoginRequest.builder()
                .email("a@b.com")
                .password("secret123")
                .build());

        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        assertThat(tokens.getUser().getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void confirmEmailMarksUserVerified() {
        UUID userId = UUID.randomUUID();
        String rawToken = "confirm-raw-token";
        EmailToken token = EmailToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(EmailTokenType.CONFIRM)
                .tokenHash(AuthService.hashToken(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        UserAccount user = UserAccount.builder()
                .id(userId)
                .email("a@b.com")
                .passwordHash(encoder.encode("secret123"))
                .emailVerified(false)
                .build();

        when(emailTokens.findByTokenHashAndType(AuthService.hashToken(rawToken), EmailTokenType.CONFIRM))
                .thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        var response = service.confirmEmail(rawToken);

        assertThat(response.getMessage()).containsIgnoringCase("confirm");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void confirmEmailRejectsExpiredOrUsedToken() {
        String rawToken = "used-token";
        EmailToken token = EmailToken.builder()
                .userId(UUID.randomUUID())
                .type(EmailTokenType.CONFIRM)
                .tokenHash(AuthService.hashToken(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .usedAt(Instant.now())
                .build();
        when(emailTokens.findByTokenHashAndType(AuthService.hashToken(rawToken), EmailTokenType.CONFIRM))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmEmail(rawToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void refreshRotatesSession() {
        UserAccount user = verifiedUser();
        String rawRefresh = "refresh-raw";
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(AuthService.hashToken(rawRefresh))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokens.findByTokenHash(AuthService.hashToken(rawRefresh))).thenReturn(Optional.of(stored));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var tokens = service.refresh(RefreshTokenRequest.builder().refreshToken(rawRefresh).build());

        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotEqualTo(rawRefresh);
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void refreshRejectsRevokedToken() {
        String rawRefresh = "revoked-refresh";
        RefreshToken stored = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(AuthService.hashToken(rawRefresh))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revokedAt(Instant.now())
                .build();
        when(refreshTokens.findByTokenHash(AuthService.hashToken(rawRefresh))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh(RefreshTokenRequest.builder().refreshToken(rawRefresh).build()))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("expirée");
    }

    @Test
    void logoutRevokesRefreshToken() {
        String rawRefresh = "logout-refresh";
        RefreshToken stored = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(AuthService.hashToken(rawRefresh))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        when(refreshTokens.findByTokenHash(AuthService.hashToken(rawRefresh))).thenReturn(Optional.of(stored));

        var response = service.logout(RefreshTokenRequest.builder().refreshToken(rawRefresh).build());

        assertThat(response.getMessage()).containsIgnoringCase("Déconnecté");
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void meAndUpdateProfile() {
        UserAccount user = verifiedUser();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        var profile = service.me(user.getId());
        assertThat(profile.getFirstName()).isEqualTo("Ada");

        var updated = service.updateMe(user.getId(), UpdateProfileRequest.builder().firstName("Ada Lovelace").build());
        assertThat(updated.getFirstName()).isEqualTo("Ada Lovelace");
        assertThat(user.getFirstName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void forgotPasswordSendsResetWhenUserExists() {
        UserAccount user = verifiedUser();
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
        when(emailTokens.save(any(EmailToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.forgotPassword(ForgotPasswordRequest.builder().email("a@b.com").build());

        assertThat(response.getMessage()).containsIgnoringCase("réinitialisation");
        verify(mailService).sendResetEmail(eq("a@b.com"), any());
    }

    @Test
    void forgotPasswordDoesNotRevealMissingAccount() {
        when(users.findByEmailIgnoreCase("missing@b.com")).thenReturn(Optional.empty());

        var response = service.forgotPassword(ForgotPasswordRequest.builder().email("missing@b.com").build());

        assertThat(response.getMessage()).containsIgnoringCase("réinitialisation");
        verify(mailService, never()).sendResetEmail(any(), any());
    }

    @Test
    void resetPasswordUpdatesHashAndMarksVerified() {
        UUID userId = UUID.randomUUID();
        String rawToken = "reset-raw";
        EmailToken token = EmailToken.builder()
                .userId(userId)
                .type(EmailTokenType.RESET)
                .tokenHash(AuthService.hashToken(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        UserAccount user = UserAccount.builder()
                .id(userId)
                .email("a@b.com")
                .passwordHash(encoder.encode("old-password"))
                .emailVerified(false)
                .build();

        when(emailTokens.findByTokenHashAndType(AuthService.hashToken(rawToken), EmailTokenType.RESET))
                .thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        var response = service.resetPassword(ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("new-secret")
                .build());

        assertThat(response.getMessage()).containsIgnoringCase("Mot de passe");
        assertThat(encoder.matches("new-secret", user.getPasswordHash())).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
    }

    private UserAccount verifiedUser() {
        return UserAccount.builder()
                .id(UUID.randomUUID())
                .email("a@b.com")
                .passwordHash(encoder.encode("secret123"))
                .emailVerified(true)
                .firstName("Ada")
                .build();
    }
}
