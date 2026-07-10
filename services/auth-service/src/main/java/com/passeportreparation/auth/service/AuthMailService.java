package com.passeportreparation.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AuthMailService {

    private static final Logger log = LoggerFactory.getLogger(AuthMailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;

    public AuthMailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled}") boolean enabled,
            @Value("${app.mail.from}") String from,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    public void sendConfirmEmail(String to, String rawToken) {
        String link = frontendUrl + "/confirmer-email?token=" + rawToken;
        send(to, "Confirme ton compte Passeport",
                "Bienvenue !\n\nConfirme ton email en ouvrant ce lien :\n" + link + "\n\nLe lien expire sous 24 h.");
    }

    public void sendResetEmail(String to, String rawToken) {
        String link = frontendUrl + "/reinitialiser-mot-de-passe?token=" + rawToken;
        send(to, "Réinitialise ton mot de passe",
                "Tu as demandé une réinitialisation.\n\nOuvre ce lien :\n" + link + "\n\nLe lien expire sous 1 h. Si ce n’est pas toi, ignore ce message.");
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("[mail-disabled] to={} subject={} body=\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            // Registration/login must not fail if SMTP is down — log the link for local/dev.
            log.warn("Email send failed (to={}, subject={}): {}. Falling back to log.\n{}",
                    to, subject, ex.getMessage(), body);
        }
    }
}
