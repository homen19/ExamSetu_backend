package com.mocktestapp.service;

import com.mocktestapp.dto.AttemptDto;
import com.mocktestapp.dto.QuestionDto;
import com.mocktestapp.entity.*;
import com.mocktestapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptService {

    private final TestAttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public AttemptDto.StartAttemptResponse startAttempt(Long testId, User user) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));

        if (test.getStatus() != Test.TestStatus.ACTIVE) {
            throw new RuntimeException("Test is not active");
        }

        // Check assignment
        assignmentRepository.findByTestIdAndUserId(testId, user.getId())
                .ifPresent(assignment -> {
                    if (!assignment.getIsAllowed()) {
                        throw new RuntimeException("Your access to this test has been restricted");
                    }
                });

        // Check attempt count
        long attemptsUsed = attemptRepository.countByUserIdAndTestId(user.getId(), testId);
        if (attemptsUsed >= test.getMaxAttempts()) {
            throw new RuntimeException("Maximum attempt limit reached");
        }

        // Check for in-progress attempt
        attemptRepository.findByUserIdAndTestIdAndStatus(user.getId(), testId, TestAttempt.AttemptStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new RuntimeException("You already have an in-progress attempt for this test");
                });

        TestAttempt attempt = TestAttempt.builder()
                .user(user)
                .test(test)
                .attemptNumber((int) attemptsUsed + 1)
                .startedAt(LocalDateTime.now())
                .status(TestAttempt.AttemptStatus.IN_PROGRESS)
                .totalMarks(test.getQuestions().stream().mapToDouble(Question::getMarks).sum())
                .build();

        TestAttempt saved = attemptRepository.save(attempt);

        // Build question response (without answers)
        List<QuestionDto.QuestionResponseNoAnswer> questions = test.getQuestions().stream()
                .sorted(Comparator.comparingInt(Question::getQuestionOrder))
                .map(q -> QuestionDto.QuestionResponseNoAnswer.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .topic(q.getTopic())
                        .marks(q.getMarks())
                        .timeLimitSeconds(q.getTimeLimitSeconds())
                        .questionOrder(q.getQuestionOrder())
                        .options(q.getOptions().stream()
                                .map(o -> QuestionDto.OptionNoAnswer.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        .optionOrder(o.getOptionOrder())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return AttemptDto.StartAttemptResponse.builder()
                .attemptId(saved.getId())
                .testId(testId)
                .testTitle(test.getTitle())
                .attemptNumber(saved.getAttemptNumber())
                .startedAt(saved.getStartedAt())
                .timerMode(test.getTimerMode())
                .totalTimerMinutes(test.getTotalTimerMinutes())
                .questions(questions)
                .build();
    }

    @Transactional
    public AttemptDto.AttemptSummary submitAttempt(Long attemptId, AttemptDto.SubmitAttemptRequest request, User user) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found: " + attemptId));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (attempt.getStatus() != TestAttempt.AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Attempt is not in progress");
        }

        Test test = attempt.getTest();
        double negativeMarking = test.getNegativeMarking();

        double totalScore = 0.0;
        int correctCount = 0;
        int wrongCount = 0;
        int skippedCount = 0;

        List<UserAnswer> userAnswers = new ArrayList<>();

        for (AttemptDto.SubmitAnswerRequest answerReq : request.getAnswers()) {
            Question question = questionRepository.findById(answerReq.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question not found: " + answerReq.getQuestionId()));

            List<Long> selectedIds = answerReq.getSelectedOptionIds();
            List<Long> correctIds = question.getOptions().stream()
                    .filter(Option::getIsCorrect)
                    .map(Option::getId)
                    .collect(Collectors.toList());

            List<Option> selectedOptions = question.getOptions().stream()
                    .filter(o -> selectedIds != null && selectedIds.contains(o.getId()))
                    .collect(Collectors.toList());

            UserAnswer.AnswerStatus answerStatus;
            double marksObtained;

            if (selectedIds == null || selectedIds.isEmpty()) {
                answerStatus = UserAnswer.AnswerStatus.SKIPPED;
                marksObtained = 0.0;
                skippedCount++;
            } else if (question.getQuestionType() == Question.QuestionType.SINGLE) {
                boolean isCorrect = selectedIds.size() == 1 && correctIds.contains(selectedIds.get(0));
                if (isCorrect) {
                    answerStatus = UserAnswer.AnswerStatus.CORRECT;
                    marksObtained = question.getMarks();
                    correctCount++;
                } else {
                    answerStatus = UserAnswer.AnswerStatus.WRONG;
                    marksObtained = negativeMarking;
                    wrongCount++;
                }
            } else {
                // MULTIPLE type scoring
                Set<Long> selectedSet = new HashSet<>(selectedIds);
                Set<Long> correctSet = new HashSet<>(correctIds);
                boolean allCorrect = selectedSet.equals(correctSet);
                boolean hasWrong = selectedIds.stream().anyMatch(id -> !correctIds.contains(id));

                if (allCorrect) {
                    answerStatus = UserAnswer.AnswerStatus.CORRECT;
                    marksObtained = question.getMarks();
                    correctCount++;
                } else if (hasWrong) {
                    answerStatus = UserAnswer.AnswerStatus.WRONG;
                    marksObtained = negativeMarking;
                    wrongCount++;
                } else {
                    // Partial - some correct selected, no wrong
                    long correctSelected = selectedIds.stream().filter(correctIds::contains).count();
                    marksObtained = (question.getMarks() / correctIds.size()) * correctSelected;
                    answerStatus = UserAnswer.AnswerStatus.PARTIAL;
                    correctCount++;
                }
            }

            totalScore += marksObtained;

            UserAnswer ua = UserAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOptions(selectedOptions)
                    .timeTakenSeconds(answerReq.getTimeTakenSeconds())
                    .answerStatus(answerStatus)
                    .marksObtained(marksObtained)
                    .build();
            userAnswers.add(ua);
        }

        // Handle questions not answered
        long answered = request.getAnswers().stream().map(AttemptDto.SubmitAnswerRequest::getQuestionId)
                .distinct().count();
        skippedCount += (int) (test.getQuestions().size() - answered);

        attempt.setScore(Math.max(0, totalScore));
        attempt.setCorrectCount(correctCount);
        attempt.setWrongCount(wrongCount);
        attempt.setSkippedCount(skippedCount);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(TestAttempt.AttemptStatus.SUBMITTED);
        
        attempt.getUserAnswers().clear();
        attempt.getUserAnswers().addAll(userAnswers);

        TestAttempt saved = attemptRepository.save(attempt);
        log.info("Attempt {} submitted. Score: {}/{}", attemptId, saved.getScore(), saved.getTotalMarks());

        return mapToSummary(saved);
    }

    public AttemptDto.AttemptDetail getAttemptDetail(Long attemptId, User user, boolean isAdmin) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found: " + attemptId));

        if (!isAdmin && !attempt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (!isAdmin && !attempt.getTest().getResultsPublished()) {
            throw new RuntimeException("Results not published yet");
        }

        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptId(attemptId);

        List<AttemptDto.AnswerDetail> answerDetails = userAnswers.stream().map(ua -> {
            List<Long> selectedIds = ua.getSelectedOptions().stream().map(Option::getId).collect(Collectors.toList());
            List<Long> correctIds = ua.getQuestion().getOptions().stream()
                    .filter(Option::getIsCorrect).map(Option::getId).collect(Collectors.toList());

            return AttemptDto.AnswerDetail.builder()
                    .questionId(ua.getQuestion().getId())
                    .questionText(ua.getQuestion().getQuestionText())
                    .topic(ua.getQuestion().getTopic())
                    .marks(ua.getQuestion().getMarks())
                    .marksObtained(ua.getMarksObtained())
                    .answerStatus(ua.getAnswerStatus())
                    .timeTakenSeconds(ua.getTimeTakenSeconds())
                    .selectedOptionIds(selectedIds)
                    .correctOptionIds(correctIds)
                    .options(ua.getQuestion().getOptions().stream()
                            .map(o -> QuestionDto.OptionResponse.builder()
                                    .id(o.getId())
                                    .optionText(o.getOptionText())
                                    .isCorrect(o.getIsCorrect())
                                    .optionOrder(o.getOptionOrder())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());

        // Topic-wise accuracy
        Map<String, AttemptDto.TopicAccuracy> topicMap = new HashMap<>();
        for (UserAnswer ua : userAnswers) {
            String topic = ua.getQuestion().getTopic() != null ? ua.getQuestion().getTopic() : "General";
            topicMap.computeIfAbsent(topic, t -> AttemptDto.TopicAccuracy.builder()
                    .topic(t).total(0L).correct(0L).build());
            AttemptDto.TopicAccuracy acc = topicMap.get(topic);
            acc.setTotal(acc.getTotal() + 1);
            if (ua.getAnswerStatus() == UserAnswer.AnswerStatus.CORRECT) {
                acc.setCorrect(acc.getCorrect() + 1);
            }
            acc.setAccuracy(acc.getTotal() > 0 ? (acc.getCorrect() * 100.0 / acc.getTotal()) : 0.0);
        }

        long durationSecs = attempt.getSubmittedAt() != null
                ? ChronoUnit.SECONDS.between(attempt.getStartedAt(), attempt.getSubmittedAt()) : 0;

        double pct = attempt.getTotalMarks() > 0
                ? (attempt.getScore() / attempt.getTotalMarks()) * 100 : 0;

        return AttemptDto.AttemptDetail.builder()
                .id(attempt.getId())
                .testId(attempt.getTest().getId())
                .testTitle(attempt.getTest().getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .score(attempt.getScore())
                .totalMarks(attempt.getTotalMarks())
                .percentage(Math.round(pct * 100.0) / 100.0)
                .correctCount(attempt.getCorrectCount())
                .wrongCount(attempt.getWrongCount())
                .skippedCount(attempt.getSkippedCount())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .durationSeconds(durationSecs)
                .answerDetails(answerDetails)
                .topicWiseAccuracy(topicMap)
                .build();
    }

    public List<AttemptDto.AttemptSummary> getMyAttempts(Long userId) {
        return attemptRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public List<AttemptDto.AttemptSummary> getAttemptsForTest(Long testId) {
        return attemptRepository.findByTestIdOrderByScoreDesc(testId).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public List<AttemptDto.LeaderboardEntry> getLeaderboard(Long testId) {
        List<TestAttempt> attempts = attemptRepository.findLeaderboardByTestId(testId);
        List<AttemptDto.LeaderboardEntry> leaderboard = new ArrayList<>();
        int rank = 1;
        for (TestAttempt a : attempts) {
            long dur = a.getSubmittedAt() != null
                    ? ChronoUnit.SECONDS.between(a.getStartedAt(), a.getSubmittedAt()) : 0;
            double pct = a.getTotalMarks() > 0 ? (a.getScore() / a.getTotalMarks()) * 100 : 0;
            leaderboard.add(AttemptDto.LeaderboardEntry.builder()
                    .rank(rank++)
                    .userId(a.getUser().getId())
                    .userName(a.getUser().getName())
                    .profilePicture(a.getUser().getProfilePicture())
                    .score(a.getScore())
                    .totalMarks(a.getTotalMarks())
                    .percentage(Math.round(pct * 100.0) / 100.0)
                    .correctCount(a.getCorrectCount())
                    .submittedAt(a.getSubmittedAt())
                    .durationSeconds(dur)
                    .build());
        }
        return leaderboard;
    }

    private AttemptDto.AttemptSummary mapToSummary(TestAttempt a) {
        long dur = a.getSubmittedAt() != null
                ? ChronoUnit.SECONDS.between(a.getStartedAt(), a.getSubmittedAt()) : 0;
        double pct = a.getTotalMarks() > 0 ? (a.getScore() / a.getTotalMarks()) * 100 : 0;

        return AttemptDto.AttemptSummary.builder()
                .id(a.getId())
                .testId(a.getTest().getId())
                .testTitle(a.getTest().getTitle())
                .attemptNumber(a.getAttemptNumber())
                .score(a.getScore())
                .totalMarks(a.getTotalMarks())
                .correctCount(a.getCorrectCount())
                .wrongCount(a.getWrongCount())
                .skippedCount(a.getSkippedCount())
                .percentage(Math.round(pct * 100.0) / 100.0)
                .status(a.getStatus())
                .startedAt(a.getStartedAt())
                .submittedAt(a.getSubmittedAt())
                .durationSeconds(dur)
                .userName(a.getUser().getName())
                .userEmail(a.getUser().getEmail())
                .build();
    }
}
