package org.example.krishantutioncenter;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-setup-flow-test;DB_CLOSE_DELAY=-1")
class AdminAccountSetupTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordResetTokenRepository resetTokens;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void firstRunSetupCreatesOneTeacherAndAllowsSignIn() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/setup"));
        mockMvc.perform(get("/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup"));
        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("displayName", "My Teacher Account")
                        .param("username", "my-teacher")
                        .param("email", "teacher@example.com")
                        .param("password", "a-private-password-2026")
                        .param("confirmPassword", "a-private-password-2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        AppUser admin = users.findByUsername("my-teacher").orElseThrow();
        assertEquals(AccountRole.ADMIN, admin.getRole());
        assertEquals("My Teacher Account", admin.getDisplayName());
        assertEquals("teacher@example.com", admin.getEmail());
        assertTrue(passwordEncoder.matches("a-private-password-2026", admin.getPassword()));
        assertEquals(1, users.countByRole(AccountRole.ADMIN));
        Instant tokenCreated = Instant.now();
        resetTokens.saveAndFlush(new PasswordResetToken(admin, passwordEncoder.encode("123456"),
                tokenCreated.plus(10, ChronoUnit.MINUTES), tokenCreated));

        mockMvc.perform(get("/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup"));
        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("displayName", "Unconfirmed Replacement")
                        .param("username", "unconfirmed")
                        .param("email", "unconfirmed@example.com")
                        .param("password", "another-private-password")
                        .param("confirmPassword", "another-private-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/setup"));
        assertTrue(users.findByUsername("my-teacher").isPresent());

        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("displayName", "New Teacher")
                        .param("username", "new-teacher")
                        .param("email", "new-teacher@example.com")
                        .param("password", "another-private-password")
                        .param("confirmPassword", "another-private-password")
                        .param("confirmReplacement", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        AppUser replacement = users.findByUsername("new-teacher").orElseThrow();
        assertEquals(admin.getId(), replacement.getId());
        assertEquals("New Teacher", replacement.getDisplayName());
        assertEquals("new-teacher@example.com", replacement.getEmail());
        assertTrue(passwordEncoder.matches("another-private-password", replacement.getPassword()));
        assertTrue(users.findByUsername("my-teacher").isEmpty());
        assertTrue(resetTokens.findByUserId(replacement.getId()).isEmpty());
        assertEquals(1, users.countByRole(AccountRole.ADMIN));

        var login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "new-teacher")
                        .param("password", "another-private-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();
        mockMvc.perform(get("/admin").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void setupScreenIsNotAvailableFromAnotherMachine() throws Exception {
        mockMvc.perform(get("/setup").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPasswordMismatchAndTooShortPassword() throws Exception {
        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("displayName", "Tutor")
                        .param("username", "tutor")
                        .param("email", "teacher@example.com")
                        .param("password", "short")
                        .param("confirmPassword", "different"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/setup"));
        assertEquals(0, users.countByRole(AccountRole.ADMIN));
    }
}
