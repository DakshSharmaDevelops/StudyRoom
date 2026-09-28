package org.example.krishantutioncenter.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @EntityGraph(attributePaths = {"batch", "questions"})
    List<Quiz> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"batch", "questions"})
    List<Quiz> findAllByBatchIdOrderByCreatedAtDesc(Long batchId);

    @EntityGraph(attributePaths = {"batch", "questions"})
    Optional<Quiz> findWithBatchAndQuestionsById(Long id);
}
