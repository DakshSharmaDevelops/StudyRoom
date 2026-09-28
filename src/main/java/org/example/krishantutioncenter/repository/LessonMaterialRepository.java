package org.example.krishantutioncenter.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.example.krishantutioncenter.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, Long> {
    @EntityGraph(attributePaths = {"batch", "tags"})
    List<LessonMaterial> findTop20ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "batch")
    List<LessonMaterial> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "batch")
    List<LessonMaterial> findAllByBatchIdOrderByCreatedAtDesc(Long batchId);

    @EntityGraph(attributePaths = {"batch", "tags"})
    List<LessonMaterial> findNotesByBatchIdOrderByCreatedAtDesc(Long batchId);

    @EntityGraph(attributePaths = "batch")
    Optional<LessonMaterial> findWithBatchById(Long id);

    @EntityGraph(attributePaths = {"batch", "tags"})
    Optional<LessonMaterial> findWithBatchAndTagsById(Long id);

    long countByType(MaterialType type);
}
