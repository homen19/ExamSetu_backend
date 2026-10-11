package com.mocktestapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "option_order")
    private Integer optionOrder;
}
