package org.example.krishantutioncenter.repository;

import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeachingBatchRepository extends JpaRepository<TeachingBatch, Long> {
    boolean existsByNameIgnoreCase(String name);
}
