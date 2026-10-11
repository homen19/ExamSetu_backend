package com.mocktestapp.dto;

import com.mocktestapp.entity.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

public class QuestionDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OptionImport {
        @NotBlank
        private String text;
        @NotNull
        private Boolean isCorrect;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionImport {
        @NotBlank
        private String questionText;
        @NotNull
        private Question.QuestionType questionType;
        private String topic;
        private Double marks;
        private Integer timeLimitSeconds;
        @NotNull
        private List<OptionImport> options;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionResponse {
        private Long id;
        private String questionText;
        private Question.QuestionType questionType;
        private String topic;
        private Double marks;
        private Integer timeLimitSeconds;
        private Integer questionOrder;
        private List<OptionResponse> options;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OptionResponse {
        private Long id;
        private String optionText;
        private Boolean isCorrect;  // Only for admin
        private Integer optionOrder;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionResponseNoAnswer {
        private Long id;
        private String questionText;
        private Question.QuestionType questionType;
        private String topic;
        private Double marks;
        private Integer timeLimitSeconds;
        private Integer questionOrder;
        private List<OptionNoAnswer> options;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OptionNoAnswer {
        private Long id;
        private String optionText;
        private Integer optionOrder;
    }
}
