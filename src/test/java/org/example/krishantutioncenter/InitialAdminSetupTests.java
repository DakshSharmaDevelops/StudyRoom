package org.example.krishantutioncenter;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-bootstrap-test;DB_CLOSE_DELAY=-1",
        "APP_ADMIN_USERNAME=bootstrap-teacher",
        "APP_ADMIN_PASSWORD=a-private-password-for-tests-2026",
        "APP_ADMIN_NAME=Bootstrap Teacher",
        "APP_ADMIN_EMAIL=bootstrap@example.test"
})
class InitialAdminSetupTests {

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InitialAdminSetup initialAdminSetup;

    @Test
    void createsHashedAdminOnlyOnceFromEnvironmentSettings() throws Exception {
        AppUser admin = users.findByUsername("bootstrap-teacher").orElseThrow();
        assertEquals(AccountRole.ADMIN, admin.getRole());
        assertEquals("Bootstrap Teacher", admin.getDisplayName());
        assertEquals("bootstrap@example.test", admin.getEmail());
        assertTrue(passwordEncoder.matches("a-private-password-for-tests-2026", admin.getPassword()));

        initialAdminSetup.run();

        assertEquals(1, users.countByRole(AccountRole.ADMIN));
    }
}
