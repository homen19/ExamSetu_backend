package com.mocktestapp.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestAnalytics {
        private Long testId;
        private String testTitle;
        private Long totalAttempts;
        private Long uniqueUsers;
        private Double averageScore;
        private Double maxScore;
        private Double minScore;
        private Double totalMarks;
        private Double averagePercentage;
        private Double passRate;
        private List<ScoreDistribution> scoreDistribution;
        private List<AttemptDto.TopicAccuracy> topicWiseAccuracy;
        private List<AttemptDto.LeaderboardEntry> topPerformers;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScoreDistribution {
        private String range;     // e.g., "0-20", "21-40"
        private Long count;
        private Double percentage;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AdminDashboard {
        private Long totalTests;
        private Long activeTests;
        private Long totalUsers;
        private Long totalAttempts;
        private List<RecentActivity> recentActivity;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentActivity {
        private String type;   // "ATTEMPT_SUBMITTED", "TEST_CREATED", "USER_REGISTERED"
        private String message;
        private LocalDateTime timestamp;
        private String userName;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserAnalytics {
        private Long userId;
        private String userName;
        private Long totalAttempts;
        private Long completedAttempts;
        private Double averageScore;
        private Double averagePercentage;
        private Double bestScore;
        private Map<String, AttemptDto.TopicAccuracy> topicStrengths;
        private List<AttemptDto.AttemptSummary> recentAttempts;
    }
}
