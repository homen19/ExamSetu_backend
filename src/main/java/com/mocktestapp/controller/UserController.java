package com.mocktestapp.controller;

import com.mocktestapp.dto.AnalyticsDto;
import com.mocktestapp.dto.AttemptDto;
import com.mocktestapp.dto.TestDto;
import com.mocktestapp.entity.User;
import com.mocktestapp.repository.TestAttemptRepository;
import com.mocktestapp.service.AnalyticsService;
import com.mocktestapp.service.AttemptService;
import com.mocktestapp.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User — Tests", description = "User endpoints for viewing assigned tests, attempting tests, and viewing results")
public class UserController {

    private final TestService testService;
    private final AttemptService attemptService;
    private final AnalyticsService analyticsService;
    private final TestAttemptRepository attemptRepository;

    // ────────────────────────────── TESTS ──────────────────────────────

    @GetMapping("/tests")
    @Operation(
            summary = "Get my assigned tests",
            description = "Returns all active tests assigned to the authenticated user, with attempt count remaining"
    )
    public ResponseEntity<List<TestDto.TestForUser>> getMyTests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(testService.getAssignedTestsForUser(user.getId()));
    }

    @GetMapping("/tests/{testId}")
    @Operation(summary = "Get test details", description = "Get test info (without answers) before starting. Must be assigned.")
    public ResponseEntity<TestDto.TestForUser> getTestById(
            @PathVariable Long testId,
            @AuthenticationPrincipal User user) {
        List<TestDto.TestForUser> tests = testService.getAssignedTestsForUser(user.getId());
        return tests.stream()
                .filter(t -> t.getId().equals(testId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    // ────────────────────────────── ATTEMPTS ──────────────────────────────

    @PostMapping("/tests/{testId}/start")
    @Operation(
            summary = "Start test attempt",
            description = """
                    Starts a new attempt for the given test. Returns all questions (WITHOUT correct answers).
                    
                    **Response includes:**
                    - `attemptId` — save this, needed to submit answers
                    - `questions[]` — all questions with options (no `isCorrect` field)
                    - `timerMode` — OVERALL or PER_QUESTION
                    - `totalTimerMinutes` — for OVERALL mode
                    - Each question has `timeLimitSeconds` — for PER_QUESTION mode
                    """
    )
    public ResponseEntity<AttemptDto.StartAttemptResponse> startTest(
            @Parameter(description = "Test ID to attempt", required = true) @PathVariable Long testId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attemptService.startAttempt(testId, user));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(
            summary = "Submit test attempt",
            description = """
                    Submit all answers for the attempt. Auto-scores immediately.
                    
                    **Request body:**
                    ```json
                    {
                      "answers": [
                        {
                          "questionId": 1,
                          "selectedOptionIds": [3],
                          "timeTakenSeconds": 45
                        },
                        {
                          "questionId": 2,
                          "selectedOptionIds": [5, 7],
                          "timeTakenSeconds": 30
                        }
                      ]
                    }
                    ```
                    - For **SINGLE** type: pass one option ID
                    - For **MULTIPLE** type: pass multiple option IDs
                    - Skip a question: pass empty `selectedOptionIds: []`
                    """
    )
    public ResponseEntity<AttemptDto.AttemptSummary> submitAttempt(
            @Parameter(description = "Attempt ID from startTest response", required = true) @PathVariable Long attemptId,
            @RequestBody AttemptDto.SubmitAttemptRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attemptService.submitAttempt(attemptId, request, user));
    }

    // ────────────────────────────── HISTORY & RESULTS ──────────────────────────────

    @GetMapping("/attempts")
    @Operation(summary = "My attempt history", description = "Returns all of the user's past test attempts sorted by date descending")
    public ResponseEntity<List<AttemptDto.AttemptSummary>> getMyAttempts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attemptService.getMyAttempts(user.getId()));
    }

    @GetMapping("/attempts/{attemptId}/result")
    @Operation(
            summary = "Get attempt result",
            description = "Full per-question result breakdown. Only available after admin publishes results for the test."
    )
    public ResponseEntity<AttemptDto.AttemptDetail> getResult(
            @Parameter(description = "Attempt ID", required = true) @PathVariable Long attemptId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attemptService.getAttemptDetail(attemptId, user, false));
    }

    @GetMapping("/leaderboard/{testId}")
    @Operation(
            summary = "Test Leaderboard",
            description = "Ranked leaderboard for a test. Only shows results if admin has published them."
    )
    public ResponseEntity<List<AttemptDto.LeaderboardEntry>> getLeaderboard(
            @PathVariable Long testId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attemptService.getLeaderboard(testId));
    }

    @GetMapping("/analytics/me")
    @Operation(summary = "My Analytics", description = "Your personal analytics: average score, best score, topic strengths, recent attempts")
    public ResponseEntity<AnalyticsDto.UserAnalytics> getMyAnalytics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(user.getId()));
    }
}
