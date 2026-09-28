package org.example.krishantutioncenter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, AppUser student, int score, int total) {
        this.quiz = quiz;
        this.student = student;
        this.score = score;
        this.total = total;
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public AppUser getStudent() {
        return student;
    }

    public int getScore() {
        return score;
    }

    public int getTotal() {
        return total;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
