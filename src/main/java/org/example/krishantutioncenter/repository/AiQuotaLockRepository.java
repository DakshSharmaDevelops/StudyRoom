package org.example.krishantutioncenter.repository;

import jakarta.persistence.LockModeType;
import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiQuotaLockRepository extends JpaRepository<AiQuotaLock, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quotaLock from AiQuotaLock quotaLock where quotaLock.id = :id")
    Optional<AiQuotaLock> findAndLock(@Param("id") int id);
}
