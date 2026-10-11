package com.mocktestapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToMany
    @JoinTable(
            name = "user_answer_options",
            joinColumns = @JoinColumn(name = "user_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    @Builder.Default
    private List<Option> selectedOptions = new ArrayList<>();

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status")
    @Builder.Default
    private AnswerStatus answerStatus = AnswerStatus.SKIPPED;

    @Column(name = "marks_obtained")
    @Builder.Default
    private Double marksObtained = 0.0;

    public enum AnswerStatus {
        CORRECT, WRONG, PARTIAL, SKIPPED
    }
}
