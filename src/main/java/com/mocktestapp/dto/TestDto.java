package com.mocktestapp.dto;

import com.mocktestapp.entity.Test;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class TestDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestImport {
        @NotBlank
        private String title;
        private String description;
        @NotNull
        private Test.TimerMode timerMode;
        private Integer totalTimerMinutes;
        private Double negativeMarking;
        private Integer maxAttempts;
        private List<String> topics;
        @NotNull
        private List<QuestionDto.QuestionImport> questions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestUpdateRequest {
        private String title;
        private String description;
        private Test.TimerMode timerMode;
        private Integer totalTimerMinutes;
        private Double negativeMarking;
        private Integer maxAttempts;
        private Test.TestStatus status;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestSummary {
        private Long id;
        private String title;
        private String description;
        private Test.TimerMode timerMode;
        private Integer totalTimerMinutes;
        private Double negativeMarking;
        private Integer maxAttempts;
        private Test.TestStatus status;
        private Boolean resultsPublished;
        private Set<String> topics;
        private Integer questionCount;
        private Long assignedUserCount;
        private LocalDateTime createdAt;
        private String createdByName;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestDetail {
        private Long id;
        private String title;
        private String description;
        private Test.TimerMode timerMode;
        private Integer totalTimerMinutes;
        private Double negativeMarking;
        private Integer maxAttempts;
        private Test.TestStatus status;
        private Boolean resultsPublished;
        private Set<String> topics;
        private List<QuestionDto.QuestionResponse> questions;
        private LocalDateTime createdAt;
        private String createdByName;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestForUser {
        private Long id;
        private String title;
        private String description;
        private Test.TimerMode timerMode;
        private Integer totalTimerMinutes;
        private Double negativeMarking;
        private Integer maxAttempts;
        private Integer attemptsUsed;
        private Integer attemptsRemaining;
        private Set<String> topics;
        private Integer questionCount;
        private Test.TestStatus status;
        private Boolean resultsPublished;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssignRequest {
        @NotNull
        private List<Long> userIds;
        private Boolean isAllowed;
    }
}
