package com.mocktestapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "score")
    @Builder.Default
    private Double score = 0.0;

    @Column(name = "total_marks")
    @Builder.Default
    private Double totalMarks = 0.0;

    @Column(name = "correct_count")
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "wrong_count")
    @Builder.Default
    private Integer wrongCount = 0;

    @Column(name = "skipped_count")
    @Builder.Default
    private Integer skippedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAnswer> userAnswers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AttemptStatus {
        IN_PROGRESS, SUBMITTED, TIMED_OUT
    }
}
