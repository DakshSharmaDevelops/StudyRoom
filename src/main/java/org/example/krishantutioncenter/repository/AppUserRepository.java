package org.example.krishantutioncenter.repository;

import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    @EntityGraph(attributePaths = "batch")
    Optional<AppUser> findWithBatchByUsername(String username);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByUsernameAndRole(String username, AccountRole role);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = "batch")
    List<AppUser> findAllByRoleOrderByDisplayName(AccountRole role);

    List<AppUser> findAllByRoleAndBatchIdOrderByDisplayName(AccountRole role, Long batchId);

    long countByRoleAndBatchId(AccountRole role, Long batchId);

    long countByRole(AccountRole role);
}
