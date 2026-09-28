package org.example.krishantutioncenter.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    @EntityGraph(attributePaths = {"quiz", "student"})
    Optional<QuizAttempt> findByIdAndStudentId(Long id, Long studentId);
}
