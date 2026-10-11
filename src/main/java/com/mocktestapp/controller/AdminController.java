package com.mocktestapp.controller;

import com.mocktestapp.dto.AnalyticsDto;
import com.mocktestapp.dto.AttemptDto;
import com.mocktestapp.dto.TestDto;
import com.mocktestapp.dto.UserDto;
import com.mocktestapp.entity.User;
import com.mocktestapp.repository.UserRepository;
import com.mocktestapp.service.AnalyticsService;
import com.mocktestapp.service.AttemptService;
import com.mocktestapp.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Tests", description = "Admin endpoints for test management, JSON import, user assignment, and analytics")
public class AdminController {

    private final TestService testService;
    private final AttemptService attemptService;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ────────────────────────────── DASHBOARD ──────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Admin Dashboard", description = "Get summary stats: total tests, active tests, user count, attempt count")
    public ResponseEntity<AnalyticsDto.AdminDashboard> getDashboard(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(analyticsService.getAdminDashboard(admin.getId()));
    }

    // ────────────────────────────── TESTS ──────────────────────────────

    @PostMapping(value = "/tests/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import test from JSON file",
            description = """
                    Upload a JSON file to create a test with all questions and options.
                    
                    **JSON Format:**
                    ```json
                    {
                      "title": "Test Name",
                      "timerMode": "OVERALL",
                      "totalTimerMinutes": 30,
                      "negativeMarking": -0.25,
                      "maxAttempts": 2,
                      "topics": ["Java", "OOP"],
                      "questions": [
                        {
                          "questionText": "What is JVM?",
                          "questionType": "SINGLE",
                          "topic": "Java",
                          "marks": 2,
                          "options": [
                            { "text": "Java Virtual Machine", "isCorrect": true },
                            { "text": "Java Variable Method", "isCorrect": false }
                          ]
                        }
                      ]
                    }
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Test imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON format"),
            @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<TestDto.TestSummary> importTest(
            @Parameter(description = "JSON file containing test data", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User admin) throws Exception {

        TestDto.TestImport importDto = objectMapper.readValue(file.getInputStream(), TestDto.TestImport.class);
        return ResponseEntity.ok(testService.importTest(importDto, admin));
    }

    @PostMapping(value = "/tests", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create test from JSON body", description = "Create a test by sending JSON directly in the request body (alternative to file upload)")
    public ResponseEntity<TestDto.TestSummary> createTest(
            @Valid @RequestBody TestDto.TestImport importDto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(testService.importTest(importDto, admin));
    }

    @GetMapping("/tests")
    @Operation(summary = "Get all tests", description = "Returns a list of all tests with summary info")
    public ResponseEntity<List<TestDto.TestSummary>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests());
    }

    @GetMapping("/tests/{testId}")
    @Operation(summary = "Get test detail", description = "Returns full test with all questions and correct answers")
    public ResponseEntity<TestDto.TestDetail> getTestDetail(
            @Parameter(description = "Test ID", required = true) @PathVariable Long testId) {
        return ResponseEntity.ok(testService.getTestDetail(testId));
    }

    @PutMapping("/tests/{testId}")
    @Operation(summary = "Update test configuration", description = "Update title, description, timer, marking scheme, max attempts, or status")
    public ResponseEntity<TestDto.TestSummary> updateTest(
            @PathVariable Long testId,
            @RequestBody TestDto.TestUpdateRequest request) {
        return ResponseEntity.ok(testService.updateTest(testId, request));
    }

    @DeleteMapping("/tests/{testId}")
    @Operation(summary = "Delete test", description = "Permanently delete a test and all its questions and attempts")
    public ResponseEntity<Void> deleteTest(@PathVariable Long testId) {
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/tests/{testId}/publish")
    @Operation(summary = "Publish results", description = "Make results visible to users for this test")
    public ResponseEntity<TestDto.TestSummary> publishResults(@PathVariable Long testId) {
        return ResponseEntity.ok(testService.publishResults(testId));
    }

    @PatchMapping("/tests/{testId}/status")
    @Operation(summary = "Change test status", description = "Set test to DRAFT, ACTIVE, or INACTIVE")
    public ResponseEntity<TestDto.TestSummary> changeStatus(
            @PathVariable Long testId,
            @RequestParam com.mocktestapp.entity.Test.TestStatus status) {
        TestDto.TestUpdateRequest req = new TestDto.TestUpdateRequest();
        req.setStatus(status);
        return ResponseEntity.ok(testService.updateTest(testId, req));
    }

    // ────────────────────────────── ASSIGNMENT ──────────────────────────────

    @PostMapping("/tests/{testId}/assign")
    @Operation(
            summary = "Assign test to users",
            description = "Assign a test to one or more users. Set `isAllowed: false` to revoke access."
    )
    public ResponseEntity<Void> assignTest(
            @PathVariable Long testId,
            @RequestBody TestDto.AssignRequest request) {
        testService.assignUsers(testId, request);
        return ResponseEntity.ok().build();
    }

    // ────────────────────────────── ATTEMPTS ──────────────────────────────

    @GetMapping("/tests/{testId}/attempts")
    @Operation(summary = "Get all attempts for a test", description = "Returns all user attempts for a specific test, sorted by score")
    public ResponseEntity<List<AttemptDto.AttemptSummary>> getTestAttempts(@PathVariable Long testId) {
        return ResponseEntity.ok(attemptService.getAttemptsForTest(testId));
    }

    @GetMapping("/attempts/{attemptId}")
    @Operation(summary = "Get attempt detail (Admin)", description = "Full per-question breakdown for any user's attempt")
    public ResponseEntity<AttemptDto.AttemptDetail> getAttemptDetail(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(attemptService.getAttemptDetail(attemptId, admin, true));
    }

    @GetMapping("/users/{userId}/attempts")
    @Operation(summary = "Get all attempts by a user", description = "Returns complete attempt history for a specific user")
    public ResponseEntity<List<AttemptDto.AttemptSummary>> getUserAttempts(@PathVariable Long userId) {
        return ResponseEntity.ok(attemptService.getMyAttempts(userId));
    }

    // ────────────────────────────── ANALYTICS ──────────────────────────────

    @GetMapping("/analytics/test/{testId}")
    @Operation(
            summary = "Test Analytics",
            description = "Full analytics for a test: score distribution, topic-wise accuracy, pass rate, top performers"
    )
    public ResponseEntity<AnalyticsDto.TestAnalytics> getTestAnalytics(@PathVariable Long testId) {
        return ResponseEntity.ok(analyticsService.getTestAnalytics(testId));
    }

    @GetMapping("/analytics/user/{userId}")
    @Operation(summary = "User Analytics", description = "Analytics for a specific user: average score, best score, topic strengths, recent attempts")
    public ResponseEntity<AnalyticsDto.UserAnalytics> getUserAnalytics(@PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(userId));
    }

    @GetMapping("/analytics/leaderboard/{testId}")
    @Operation(summary = "Test Leaderboard", description = "Ranked list of all users who submitted the test, sorted by score descending")
    public ResponseEntity<List<AttemptDto.LeaderboardEntry>> getLeaderboard(@PathVariable Long testId) {
        return ResponseEntity.ok(attemptService.getLeaderboard(testId));
    }

    // ────────────────────────────── USERS ──────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Returns list of all registered users")
    public ResponseEntity<List<UserDto.UserResponse>> getAllUsers() {
        List<UserDto.UserResponse> users = userRepository.findAll().stream()
                .map(u -> UserDto.UserResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .profilePicture(u.getProfilePicture())
                        .role(u.getRole())
                        .isActive(u.getIsActive())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Update user role", description = "Promote a user to ADMIN or demote to USER")
    public ResponseEntity<UserDto.UserResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestParam User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        user = userRepository.save(user);
        return ResponseEntity.ok(UserDto.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build());
    }
}
