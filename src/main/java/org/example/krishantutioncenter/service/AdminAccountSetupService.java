package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;

@Service
public class AdminAccountSetupService {

    private final AppUserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactions;

    public AdminAccountSetupService(AppUserRepository users, PasswordResetTokenRepository resetTokens,
                                    PasswordEncoder passwordEncoder,
                                    org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public synchronized void createInitialAdmin(String displayName, String username, String password, String email) {
        transactions.executeWithoutResult(transaction -> {
            if (users.countByRole(AccountRole.ADMIN) > 0) {
                throw new IllegalStateException("The teacher account has already been set up.");
            }
            String cleanUsername = username.trim().toLowerCase(Locale.ROOT);
            if (users.existsByUsername(cleanUsername)) {
                throw new IllegalArgumentException("That username is already in use.");
            }
            users.saveAndFlush(new AppUser(cleanUsername, displayName.trim(), passwordEncoder.encode(password),
                    AccountRole.ADMIN, null, email.trim().toLowerCase(Locale.ROOT)));
        });
    }

    public synchronized void replaceTeacherLogin(String displayName, String username, String password, String email) {
        transactions.executeWithoutResult(transaction -> {
            var admins = users.findAllByRoleOrderByDisplayName(AccountRole.ADMIN);
            if (admins.size() != 1) {
                throw new IllegalStateException("Teacher login replacement is available only for a single teacher account.");
            }
            String cleanUsername = username.trim().toLowerCase(Locale.ROOT);
            AppUser admin = admins.get(0);
            if (!admin.getUsername().equals(cleanUsername) && users.existsByUsername(cleanUsername)) {
                throw new IllegalArgumentException("That username is already in use.");
            }
            resetTokens.deleteByUserId(admin.getId());
            admin.updateTeacherLogin(cleanUsername, displayName.trim(),
                    email.trim().toLowerCase(Locale.ROOT), passwordEncoder.encode(password));
            users.saveAndFlush(admin);
        });
    }
}
