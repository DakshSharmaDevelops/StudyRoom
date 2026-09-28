package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    private static final Duration RESEND_WAIT = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 5;

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String mailHost;
    private final String fromAddress;

    public PasswordResetService(AppUserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder, ObjectProvider<JavaMailSender> mailSenders,
                                @Value("${spring.mail.host:}") String mailHost,
                                @Value("${app.password-reset.from:}") String fromAddress) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailSenders = mailSenders;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public synchronized void sendCode(String username) {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailHost.isBlank() || fromAddress.isBlank() || mailSender == null) {
            throw new PasswordResetDeliveryException("Email delivery is not configured.", null);
        }
        AppUser user = users.findByUsernameAndRole(username.trim().toLowerCase(Locale.ROOT), AccountRole.ADMIN)
                .orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        Instant now = Instant.now();
        PasswordResetToken existing = tokens.findByUserId(user.getId()).orElse(null);
        if (existing != null && existing.getCreatedAt().plus(RESEND_WAIT).isAfter(now)) {
            return;
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        PasswordResetToken token;
        if (existing == null) {
            token = new PasswordResetToken(user, codeHash, now.plus(CODE_LIFETIME), now);
        } else {
            existing.replaceCode(codeHash, now.plus(CODE_LIFETIME), now);
            token = existing;
        }
        tokens.saveAndFlush(token);
        sendEmail(mailSender, fromAddress, user.getEmail(), code);
    }

    @Transactional
    public synchronized boolean resetPassword(String username, String code, String newPassword) {
        AppUser user = users.findByUsernameAndRole(username.trim().toLowerCase(Locale.ROOT), AccountRole.ADMIN)
                .orElse(null);
        if (user == null) {
            return false;
        }
        PasswordResetToken token = tokens.findByUserId(user.getId()).orElse(null);
        if (token == null) {
            return false;
        }
        Instant now = Instant.now();
        if (!token.getExpiresAt().isAfter(now) || token.getAttempts() >= MAX_ATTEMPTS) {
            tokens.delete(token);
            return false;
        }
        if (!code.matches("[0-9]{6}") || !passwordEncoder.matches(code, token.getCodeHash())) {
            token.recordFailedAttempt();
            tokens.save(token);
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);
        tokens.delete(token);
        return true;
    }

    private void sendEmail(JavaMailSender sender, String from, String recipient, String code) {
        MimeMessage message = sender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("Your Studyroom password reset code");
            helper.setText("Your password reset code is " + code
                    + ". It expires in 10 minutes. If you did not request this, ignore this email.", false);
        } catch (MessagingException exception) {
            throw new PasswordResetDeliveryException("Unable to prepare the password reset email.", exception);
        }
        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new PasswordResetDeliveryException("Unable to send the password reset email.", exception);
        }
    }
}
