package com.passeportreparation.auth.service;

import com.passeportreparation.auth.entity.EmailToken;
import com.passeportreparation.auth.entity.EmailTokenType;
import com.passeportreparation.auth.entity.RefreshToken;
import com.passeportreparation.auth.entity.UserAccount;
import com.passeportreparation.auth.repository.EmailTokenRepository;
import com.passeportreparation.auth.repository.RefreshTokenRepository;
import com.passeportreparation.auth.repository.UserAccountRepository;
import com.passeportreparation.auth.security.JwtService;
import com.passeportreparation.common.dto.LoginRequest;
import com.passeportreparation.common.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void loginReturnsTokensWhenVerified() {
        UserAccount user = UserAccount.builder()
                .id(UUID.randomUUID())
                .email("a@b.com")
                .passwordHash(encoder.encode("secret123"))
                .emailVerified(true)
                .firstName("Ada")
                .build();
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
}
