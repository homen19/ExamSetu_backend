package com.mocktestapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "timer_mode", nullable = false)
    @Builder.Default
    private TimerMode timerMode = TimerMode.OVERALL;

    @Column(name = "total_timer_minutes")
    private Integer totalTimerMinutes;

    @Column(name = "negative_marking")
    @Builder.Default
    private Double negativeMarking = 0.0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.DRAFT;

    @Column(name = "results_published")
    @Builder.Default
    private Boolean resultsPublished = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "test_topics",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    @Builder.Default
    private Set<Topic> topics = new HashSet<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestAttempt> attempts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TimerMode {
        OVERALL, PER_QUESTION
    }

    public enum TestStatus {
        DRAFT, ACTIVE, INACTIVE
    }
}
