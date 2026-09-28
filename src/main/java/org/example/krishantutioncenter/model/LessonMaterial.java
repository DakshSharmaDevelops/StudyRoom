package org.example.krishantutioncenter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_materials")
public class LessonMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false, unique = true, length = 80)
    private String storageKey;

    @Column(nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MaterialType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TeachingBatch batch;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(columnDefinition = "TEXT")
    private String englishSummary;

    @Column(columnDefinition = "TEXT")
    private String hindiSummary;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean aiSummaryPublished;

    @ElementCollection
    @CollectionTable(name = "lesson_material_tags", joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "tag", nullable = false, length = 50)
    @OrderColumn(name = "tag_order")
    private List<String> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected LessonMaterial() {
    }

    public LessonMaterial(String title, String originalFilename, String contentType, String storageKey,
                          long sizeBytes, MaterialType type, TeachingBatch batch, String extractedText) {
        this.title = title;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes;
        this.type = type;
        this.batch = batch;
        this.extractedText = extractedText;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public MaterialType getType() {
        return type;
    }

    public TeachingBatch getBatch() {
        return batch;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public String getEnglishSummary() {
        return englishSummary;
    }

    public String getHindiSummary() {
        return hindiSummary;
    }

    public List<String> getTags() {
        return List.copyOf(tags);
    }

    public boolean isAiSummaryPublished() {
        return aiSummaryPublished;
    }

    public void setAiMetadata(String englishSummary, String hindiSummary, List<String> tags) {
        this.englishSummary = englishSummary;
        this.hindiSummary = hindiSummary;
        this.tags.clear();
        this.tags.addAll(tags);
        this.aiSummaryPublished = false;
    }

    public void reviewAiMetadata(String englishSummary, String hindiSummary, List<String> tags,
                                 boolean published) {
        this.englishSummary = englishSummary;
        this.hindiSummary = hindiSummary;
        this.tags.clear();
        this.tags.addAll(tags);
        this.aiSummaryPublished = published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
