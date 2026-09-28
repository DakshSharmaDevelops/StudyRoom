package org.example.krishantutioncenter;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-reset-test;DB_CLOSE_DELAY=-1",
        "spring.mail.host=smtp.example.test",
        "app.password-reset.from=studyroom@example.test"
})
class PasswordResetTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordResetTokenRepository tokens;

    @Autowired
    private PasswordResetService passwordReset;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void createTeacherAccount() {
        tokens.deleteAll();
        users.deleteAll();
        users.saveAndFlush(new AppUser("recovery-teacher", "Recovery Teacher",
                passwordEncoder.encode("an-old-private-password"), AccountRole.ADMIN, null,
                "teacher@example.test"));
    }

    @Test
    void sendsHashedCodeAndChangesPasswordOnlyAfterValidCode() throws Exception {
        doReturn(new MimeMessage(Session.getInstance(new Properties())))
                .when(mailSender).createMimeMessage();
        doNothing().when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("username", "recovery-teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));

        MimeMessage message = lastSentMessage();
        String emailText = message.getContent().toString();
        Matcher matcher = Pattern.compile("\\b([0-9]{6})\\b").matcher(emailText);
        assertTrue(matcher.find());
        String code = matcher.group(1);
        PasswordResetToken token = tokens.findByUserId(
                users.findByUsername("recovery-teacher").orElseThrow().getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(code, token.getCodeHash()));
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("username", "recovery-teacher")
                        .param("code", wrongCode)
                        .param("password", "a-new-private-password-2026")
                        .param("confirmPassword", "a-new-private-password-2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password"));
        assertTrue(passwordEncoder.matches("an-old-private-password",
                users.findByUsername("recovery-teacher").orElseThrow().getPassword()));

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("username", "recovery-teacher")
                        .param("code", code)
                        .param("password", "a-new-private-password-2026")
                        .param("confirmPassword", "a-new-private-password-2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        AppUser updated = users.findByUsername("recovery-teacher").orElseThrow();
        assertEquals(AccountRole.ADMIN, updated.getRole());
        assertTrue(passwordEncoder.matches("a-new-private-password-2026", updated.getPassword()));
        assertTrue(tokens.findByUserId(updated.getId()).isEmpty());
    }

    @Test
    void unknownUsernameDoesNotSendEmail() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("username", "no-such-teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));
        org.mockito.Mockito.verifyNoInteractions(mailSender);
        assertTrue(tokens.findAll().isEmpty());
    }

    @Test
    void mailFailureKeepsResetRequestResponseNeutral() throws Exception {
        doReturn(new MimeMessage(Session.getInstance(new Properties())))
                .when(mailSender).createMimeMessage();
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        var result = mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("username", "recovery-teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andReturn();

        assertTrue(result.getFlashMap().get("notice").toString().contains("If a teacher account"));
        assertTrue(tokens.findAll().isEmpty());
    }

    @Test
    void invalidCodesAreLimitedToFiveAttempts() {
        AppUser admin = users.findByUsername("recovery-teacher").orElseThrow();
        Instant now = Instant.now();
        tokens.saveAndFlush(new PasswordResetToken(admin, passwordEncoder.encode("123456"),
                now.plus(10, ChronoUnit.MINUTES), now));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(passwordReset.resetPassword("recovery-teacher", "654321",
                    "a-new-private-password-2026"));
        }
        assertFalse(passwordReset.resetPassword("recovery-teacher", "123456",
                "a-new-private-password-2026"));
        assertTrue(tokens.findByUserId(admin.getId()).isEmpty());
        assertTrue(passwordEncoder.matches("an-old-private-password",
                users.findByUsername("recovery-teacher").orElseThrow().getPassword()));
    }

    private MimeMessage lastSentMessage() throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
