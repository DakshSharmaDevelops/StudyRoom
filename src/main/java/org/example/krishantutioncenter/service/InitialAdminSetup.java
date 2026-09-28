package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class InitialAdminSetup implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitialAdminSetup.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public InitialAdminSetup(AppUserRepository users, PasswordEncoder passwordEncoder, Environment environment) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        String configuredUsername = environment.getProperty("APP_ADMIN_USERNAME");
        String password = environment.getProperty("APP_ADMIN_PASSWORD");
        String displayName = environment.getProperty("APP_ADMIN_NAME", "Tutor");
        String email = environment.getProperty("APP_ADMIN_EMAIL", "").trim().toLowerCase(Locale.ROOT);
        if (configuredUsername == null || configuredUsername.isBlank()) {
            logger.warn("Admin account not initialized. Set APP_ADMIN_USERNAME.");
            return;
        }
        String username = configuredUsername.trim().toLowerCase(Locale.ROOT);
        AppUser existingUser = users.findByUsernameAndRole(username, AccountRole.ADMIN).orElse(null);
        if (existingUser != null) {
            if ((existingUser.getEmail() == null || existingUser.getEmail().isBlank())
                    && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                existingUser.setEmail(email);
                users.save(existingUser);
                logger.info("Recovery email configured for admin account '{}'.", username);
            }
            return;
        }
        if (password == null || password.length() < 12) {
            logger.warn("Admin account not initialized. Set a 12+ character APP_ADMIN_PASSWORD.");
            return;
        }
        if (email.isBlank() || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            logger.warn("Admin account not initialized. Set APP_ADMIN_EMAIL to a valid recovery email.");
            return;
        }
        String cleanDisplayName = displayName.isBlank() ? "Tutor" : displayName.trim();
        users.save(new AppUser(username, cleanDisplayName, passwordEncoder.encode(password),
                AccountRole.ADMIN, null, email));
        logger.info("Initial admin account '{}' created from environment configuration.", username);
    }
}
