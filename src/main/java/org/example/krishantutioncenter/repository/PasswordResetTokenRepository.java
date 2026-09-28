package org.example.krishantutioncenter.repository;

import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
