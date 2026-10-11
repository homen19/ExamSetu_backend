package com.mocktestapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "topic")
    private String topic;

    @Column(nullable = false)
    @Builder.Default
    private Double marks = 1.0;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "question_order")
    private Integer questionOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Option> options = new ArrayList<>();

    public enum QuestionType {
        SINGLE, MULTIPLE
    }
}
