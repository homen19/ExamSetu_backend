package com.mocktestapp.service;

import com.mocktestapp.dto.TestDto;
import com.mocktestapp.dto.QuestionDto;
import com.mocktestapp.entity.*;
import com.mocktestapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public TestDto.TestSummary importTest(TestDto.TestImport importDto, User admin) {
        // Resolve topics
        Set<Topic> topics = new HashSet<>();
        if (importDto.getTopics() != null) {
            for (String topicName : importDto.getTopics()) {
                Topic topic = topicRepository.findByNameIgnoreCase(topicName)
                        .orElseGet(() -> topicRepository.save(Topic.builder().name(topicName).build()));
                topics.add(topic);
            }
        }

        Test test = Test.builder()
                .title(importDto.getTitle())
                .description(importDto.getDescription())
                .timerMode(importDto.getTimerMode())
                .totalTimerMinutes(importDto.getTotalTimerMinutes())
                .negativeMarking(importDto.getNegativeMarking() != null ? importDto.getNegativeMarking() : 0.0)
                .maxAttempts(importDto.getMaxAttempts() != null ? importDto.getMaxAttempts() : 1)
                .status(Test.TestStatus.DRAFT)
                .createdBy(admin)
                .topics(topics)
                .build();

        // Build questions
        List<Question> questions = new ArrayList<>();
        int qOrder = 1;
        for (QuestionDto.QuestionImport qImport : importDto.getQuestions()) {
            Question question = Question.builder()
                    .test(test)
                    .questionText(qImport.getQuestionText())
                    .questionType(qImport.getQuestionType())
                    .topic(qImport.getTopic())
                    .marks(qImport.getMarks() != null ? qImport.getMarks() : 1.0)
                    .timeLimitSeconds(qImport.getTimeLimitSeconds())
                    .questionOrder(qOrder++)
                    .build();

            // Build options
            List<Option> options = new ArrayList<>();
            int oOrder = 1;
            if (qImport.getOptions() != null) {
                for (QuestionDto.OptionImport oImport : qImport.getOptions()) {
                    Option option = Option.builder()
                            .question(question)
                            .optionText(oImport.getText())
                            .isCorrect(oImport.getIsCorrect())
                            .optionOrder(oOrder++)
                            .build();
                    options.add(option);
                }
            }
            question.setOptions(options);
            questions.add(question);
        }
        test.setQuestions(questions);

        Test saved = testRepository.save(test);
        log.info("Test imported: {} with {} questions", saved.getTitle(), questions.size());
        return mapToSummary(saved);
    }

    public List<TestDto.TestSummary> getAllTests() {
        return testRepository.findAll().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public TestDto.TestDetail getTestDetail(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));
        return mapToDetail(test);
    }

    @Transactional
    public TestDto.TestSummary updateTest(Long testId, TestDto.TestUpdateRequest req) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));

        if (req.getTitle() != null) test.setTitle(req.getTitle());
        if (req.getDescription() != null) test.setDescription(req.getDescription());
        if (req.getTimerMode() != null) test.setTimerMode(req.getTimerMode());
        if (req.getTotalTimerMinutes() != null) test.setTotalTimerMinutes(req.getTotalTimerMinutes());
        if (req.getNegativeMarking() != null) test.setNegativeMarking(req.getNegativeMarking());
        if (req.getMaxAttempts() != null) test.setMaxAttempts(req.getMaxAttempts());
        if (req.getStatus() != null) test.setStatus(req.getStatus());

        return mapToSummary(testRepository.save(test));
    }

    @Transactional
    public void deleteTest(Long testId) {
        testRepository.deleteById(testId);
    }

    @Transactional
    public TestDto.TestSummary publishResults(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));
        test.setResultsPublished(true);
        return mapToSummary(testRepository.save(test));
    }

    @Transactional
    public void assignUsers(Long testId, TestDto.AssignRequest req) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));

        boolean isAllowed = req.getIsAllowed() != null ? req.getIsAllowed() : true;

        for (Long userId : req.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            assignmentRepository.findByTestIdAndUserId(testId, userId).ifPresentOrElse(
                    existing -> {
                        existing.setIsAllowed(isAllowed);
                        assignmentRepository.save(existing);
                    },
                    () -> assignmentRepository.save(TestAssignment.builder()
                            .test(test)
                            .user(user)
                            .isAllowed(isAllowed)
                            .build())
            );
        }
    }

    public List<TestDto.TestForUser> getAssignedTestsForUser(Long userId) {
        List<Test> tests = testRepository.findAssignedActiveTestsForUser(userId);
        return tests.stream()
                .map(t -> mapToTestForUser(t, userId))
                .collect(Collectors.toList());
    }

    private TestDto.TestForUser mapToTestForUser(Test test, Long userId) {
        return TestDto.TestForUser.builder()
                .id(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .timerMode(test.getTimerMode())
                .totalTimerMinutes(test.getTotalTimerMinutes())
                .negativeMarking(test.getNegativeMarking())
                .maxAttempts(test.getMaxAttempts())
                .attemptsUsed(0)
                .attemptsRemaining(test.getMaxAttempts())
                .topics(test.getTopics().stream().map(Topic::getName).collect(Collectors.toSet()))
                .questionCount(test.getQuestions().size())
                .status(test.getStatus())
                .resultsPublished(test.getResultsPublished())
                .build();
    }

    public TestDto.TestSummary mapToSummary(Test test) {
        return TestDto.TestSummary.builder()
                .id(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .timerMode(test.getTimerMode())
                .totalTimerMinutes(test.getTotalTimerMinutes())
                .negativeMarking(test.getNegativeMarking())
                .maxAttempts(test.getMaxAttempts())
                .status(test.getStatus())
                .resultsPublished(test.getResultsPublished())
                .topics(test.getTopics().stream().map(Topic::getName).collect(Collectors.toSet()))
                .questionCount(test.getQuestions().size())
                .assignedUserCount(assignmentRepository.countByTestId(test.getId()))
                .createdAt(test.getCreatedAt())
                .createdByName(test.getCreatedBy().getName())
                .build();
    }

    public TestDto.TestDetail mapToDetail(Test test) {
        List<QuestionDto.QuestionResponse> questions = test.getQuestions().stream()
                .map(q -> QuestionDto.QuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .topic(q.getTopic())
                        .marks(q.getMarks())
                        .timeLimitSeconds(q.getTimeLimitSeconds())
                        .questionOrder(q.getQuestionOrder())
                        .options(q.getOptions().stream()
                                .map(o -> QuestionDto.OptionResponse.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        .isCorrect(o.getIsCorrect())
                                        .optionOrder(o.getOptionOrder())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return TestDto.TestDetail.builder()
                .id(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .timerMode(test.getTimerMode())
                .totalTimerMinutes(test.getTotalTimerMinutes())
                .negativeMarking(test.getNegativeMarking())
                .maxAttempts(test.getMaxAttempts())
                .status(test.getStatus())
                .resultsPublished(test.getResultsPublished())
                .topics(test.getTopics().stream().map(Topic::getName).collect(Collectors.toSet()))
                .questions(questions)
                .createdAt(test.getCreatedAt())
                .createdByName(test.getCreatedBy().getName())
                .build();
    }
}
