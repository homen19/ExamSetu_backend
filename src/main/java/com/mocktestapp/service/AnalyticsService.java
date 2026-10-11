package com.mocktestapp.service;

import com.mocktestapp.dto.AnalyticsDto;
import com.mocktestapp.dto.AttemptDto;
import com.mocktestapp.entity.TestAttempt;
import com.mocktestapp.entity.User;
import com.mocktestapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TestAttemptRepository attemptRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final UserAnswerRepository userAnswerRepository;

    public AnalyticsDto.AdminDashboard getAdminDashboard(Long adminId) {
        long totalTests = testRepository.countByCreatedById(adminId);
        long activeTests = testRepository.findByStatus(com.mocktestapp.entity.Test.TestStatus.ACTIVE).stream()
                .filter(t -> t.getCreatedBy().getId().equals(adminId)).count();
        long totalUsers = userRepository.count() - 1; // exclude admin
        long totalAttempts = attemptRepository.count();

        return AnalyticsDto.AdminDashboard.builder()
                .totalTests(totalTests)
                .activeTests(activeTests)
                .totalUsers(totalUsers)
                .totalAttempts(totalAttempts)
                .recentActivity(List.of())
                .build();
    }

    public AnalyticsDto.TestAnalytics getTestAnalytics(Long testId) {
        var test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        Double avgScore = attemptRepository.findAverageScoreByTestId(testId);
        Double maxScore = attemptRepository.findMaxScoreByTestId(testId);
        Double minScore = attemptRepository.findMinScoreByTestId(testId);
        long totalAttempts = attemptRepository.countByTestId(testId);
        long uniqueUsers = attemptRepository.countDistinctUsersByTestId(testId);

        double totalMarks = test.getQuestions().stream().mapToDouble(q -> q.getMarks()).sum();

        // Score distribution
        List<TestAttempt> allAttempts = attemptRepository.findLeaderboardByTestId(testId);
        List<AnalyticsDto.ScoreDistribution> distribution = buildScoreDistribution(allAttempts, totalMarks);

        // Topic-wise accuracy
        List<Object[]> topicRows = userAnswerRepository.findTopicWiseAccuracyByTestId(testId);
        List<AttemptDto.TopicAccuracy> topicAccuracy = topicRows.stream().map(row -> {
            String topic = (String) row[0];
            Long total = ((Number) row[1]).longValue();
            Long correct = ((Number) row[2]).longValue();
            double acc = total > 0 ? (correct * 100.0 / total) : 0;
            return AttemptDto.TopicAccuracy.builder()
                    .topic(topic != null ? topic : "General")
                    .total(total)
                    .correct(correct)
                    .accuracy(Math.round(acc * 100.0) / 100.0)
                    .build();
        }).collect(Collectors.toList());

        // Top performers
        List<AttemptDto.LeaderboardEntry> top5 = allAttempts.stream().limit(5).map((a) -> {
            long dur = a.getSubmittedAt() != null
                    ? ChronoUnit.SECONDS.between(a.getStartedAt(), a.getSubmittedAt()) : 0;
            double pct = totalMarks > 0 ? (a.getScore() / totalMarks) * 100 : 0;
            return AttemptDto.LeaderboardEntry.builder()
                    .userId(a.getUser().getId())
                    .userName(a.getUser().getName())
                    .profilePicture(a.getUser().getProfilePicture())
                    .score(a.getScore())
                    .totalMarks(totalMarks)
                    .percentage(Math.round(pct * 100.0) / 100.0)
                    .correctCount(a.getCorrectCount())
                    .submittedAt(a.getSubmittedAt())
                    .durationSeconds(dur)
                    .build();
        }).collect(Collectors.toList());

        double avgPct = totalMarks > 0 && avgScore != null ? (avgScore / totalMarks) * 100 : 0;
        long passCount = allAttempts.stream().filter(a -> totalMarks > 0 && (a.getScore() / totalMarks) >= 0.4).count();
        double passRate = totalAttempts > 0 ? (passCount * 100.0 / totalAttempts) : 0;

        return AnalyticsDto.TestAnalytics.builder()
                .testId(testId)
                .testTitle(test.getTitle())
                .totalAttempts(totalAttempts)
                .uniqueUsers(uniqueUsers)
                .averageScore(avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0)
                .maxScore(maxScore != null ? maxScore : 0)
                .minScore(minScore != null ? minScore : 0)
                .totalMarks(totalMarks)
                .averagePercentage(Math.round(avgPct * 100.0) / 100.0)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .scoreDistribution(distribution)
                .topicWiseAccuracy(topicAccuracy)
                .topPerformers(top5)
                .build();
    }

    public AnalyticsDto.UserAnalytics getUserAnalytics(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TestAttempt> attempts = attemptRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long completed = attempts.stream().filter(a -> a.getStatus() == TestAttempt.AttemptStatus.SUBMITTED).count();
        OptionalDouble avg = attempts.stream().filter(a -> a.getStatus() == TestAttempt.AttemptStatus.SUBMITTED)
                .mapToDouble(a -> a.getTotalMarks() > 0 ? (a.getScore() / a.getTotalMarks()) * 100 : 0).average();
        OptionalDouble best = attempts.stream().filter(a -> a.getStatus() == TestAttempt.AttemptStatus.SUBMITTED)
                .mapToDouble(TestAttempt::getScore).max();

        return AnalyticsDto.UserAnalytics.builder()
                .userId(userId)
                .userName(user.getName())
                .totalAttempts((long) attempts.size())
                .completedAttempts(completed)
                .averageScore(0.0)
                .averagePercentage(avg.isPresent() ? Math.round(avg.getAsDouble() * 100.0) / 100.0 : 0)
                .bestScore(best.isPresent() ? best.getAsDouble() : 0)
                .topicStrengths(Map.of())
                .recentAttempts(attempts.stream().limit(5).map(a -> {
                    double pct = a.getTotalMarks() > 0 ? (a.getScore() / a.getTotalMarks()) * 100 : 0;
                    long dur = a.getSubmittedAt() != null
                            ? ChronoUnit.SECONDS.between(a.getStartedAt(), a.getSubmittedAt()) : 0;
                    return AttemptDto.AttemptSummary.builder()
                            .id(a.getId())
                            .testId(a.getTest().getId())
                            .testTitle(a.getTest().getTitle())
                            .attemptNumber(a.getAttemptNumber())
                            .score(a.getScore())
                            .totalMarks(a.getTotalMarks())
                            .percentage(Math.round(pct * 100.0) / 100.0)
                            .correctCount(a.getCorrectCount())
                            .wrongCount(a.getWrongCount())
                            .skippedCount(a.getSkippedCount())
                            .status(a.getStatus())
                            .startedAt(a.getStartedAt())
                            .submittedAt(a.getSubmittedAt())
                            .durationSeconds(dur)
                            .userName(user.getName())
                            .userEmail(user.getEmail())
                            .build();
                }).collect(Collectors.toList()))
                .build();
    }

    private List<AnalyticsDto.ScoreDistribution> buildScoreDistribution(List<TestAttempt> attempts, double totalMarks) {
        String[] ranges = {"0-20", "21-40", "41-60", "61-80", "81-100"};
        long[] counts = new long[5];
        for (TestAttempt a : attempts) {
            double pct = totalMarks > 0 ? (a.getScore() / totalMarks) * 100 : 0;
            int bucket = Math.min((int)(pct / 20), 4);
            counts[bucket]++;
        }
        List<AnalyticsDto.ScoreDistribution> dist = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double pct = attempts.size() > 0 ? (counts[i] * 100.0 / attempts.size()) : 0;
            dist.add(AnalyticsDto.ScoreDistribution.builder()
                    .range(ranges[i] + "%")
                    .count(counts[i])
                    .percentage(Math.round(pct * 100.0) / 100.0)
                    .build());
        }
        return dist;
    }
}
