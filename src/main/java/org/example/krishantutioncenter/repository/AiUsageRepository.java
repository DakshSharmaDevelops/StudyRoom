package org.example.krishantutioncenter.repository;

import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiUsageRepository extends JpaRepository<AiUsage, String> {
    Optional<AiUsage> findByMonthKey(String monthKey);
}
