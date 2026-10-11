package com.mocktestapp.repository;

import com.mocktestapp.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    List<UserAnswer> findByAttemptId(Long attemptId);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.attempt.id = :attemptId AND ua.question.id = :questionId")
    java.util.Optional<UserAnswer> findByAttemptIdAndQuestionId(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Long questionId
    );

    @Query("SELECT ua.question.topic, COUNT(ua), SUM(CASE WHEN ua.answerStatus = 'CORRECT' THEN 1 ELSE 0 END) " +
           "FROM UserAnswer ua WHERE ua.attempt.id = :attemptId GROUP BY ua.question.topic")
    List<Object[]> findTopicWiseAccuracyByAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT ua.question.topic, COUNT(ua), SUM(CASE WHEN ua.answerStatus = 'CORRECT' THEN 1 ELSE 0 END) " +
           "FROM UserAnswer ua WHERE ua.attempt.test.id = :testId AND ua.attempt.status = 'SUBMITTED' GROUP BY ua.question.topic")
    List<Object[]> findTopicWiseAccuracyByTestId(@Param("testId") Long testId);
}
