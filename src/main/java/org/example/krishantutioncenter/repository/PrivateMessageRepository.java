package org.example.krishantutioncenter.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<PrivateMessage> findAllByStudentIdOrderByCreatedAtAsc(Long studentId);

    @EntityGraph(attributePaths = {"sender", "student"})
    List<PrivateMessage> findTop100ByOrderByCreatedAtDesc();
}
