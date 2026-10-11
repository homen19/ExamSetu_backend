package com.mocktestapp.dto;

import com.mocktestapp.entity.Test;
import com.mocktestapp.entity.TestAttempt;
import com.mocktestapp.entity.UserAnswer;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AttemptDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StartAttemptResponse {
        private Long attemptId;
        private Long testId;
        private String testTitle;
        private Integer attemptNumber;
        private LocalDateTime startedAt;
        private Test.TimerMode timerMode;
        private Integer totalTimerMinutes;
        private List<QuestionDto.QuestionResponseNoAnswer> questions;

        public enum TimerMode { OVERALL, PER_QUESTION }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubmitAnswerRequest {
        private Long questionId;
        private List<Long> selectedOptionIds;
        private Integer timeTakenSeconds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubmitAttemptRequest {
        private List<SubmitAnswerRequest> answers;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttemptSummary {
        private Long id;
        private Long testId;
        private String testTitle;
        private Integer attemptNumber;
        private Double score;
        private Double totalMarks;
        private Integer correctCount;
        private Integer wrongCount;
        private Integer skippedCount;
        private Double percentage;
        private TestAttempt.AttemptStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private Long durationSeconds;
        private String userName;
        private String userEmail;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttemptDetail {
        private Long id;
        private Long testId;
        private String testTitle;
        private Integer attemptNumber;
        private Double score;
        private Double totalMarks;
        private Double percentage;
        private Integer correctCount;
        private Integer wrongCount;
        private Integer skippedCount;
        private TestAttempt.AttemptStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private Long durationSeconds;
        private List<AnswerDetail> answerDetails;
        private Map<String, TopicAccuracy> topicWiseAccuracy;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AnswerDetail {
        private Long questionId;
        private String questionText;
        private String topic;
        private Double marks;
        private Double marksObtained;
        private UserAnswer.AnswerStatus answerStatus;
        private Integer timeTakenSeconds;
        private List<QuestionDto.OptionResponse> options;
        private List<Long> selectedOptionIds;
        private List<Long> correctOptionIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopicAccuracy {
        private String topic;
        private Long total;
        private Long correct;
        private Double accuracy;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeaderboardEntry {
        private Integer rank;
        private Long userId;
        private String userName;
        private String profilePicture;
        private Double score;
        private Double totalMarks;
        private Double percentage;
        private Integer correctCount;
        private LocalDateTime submittedAt;
        private Long durationSeconds;
    }
}
