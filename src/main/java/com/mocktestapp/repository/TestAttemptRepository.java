package com.mocktestapp.repository;

import com.mocktestapp.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    List<TestAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TestAttempt> findByTestIdOrderByScoreDesc(Long testId);

    List<TestAttempt> findByUserIdAndTestId(Long userId, Long testId);

    Optional<TestAttempt> findByUserIdAndTestIdAndStatus(Long userId, Long testId, TestAttempt.AttemptStatus status);

    long countByUserIdAndTestId(Long userId, Long testId);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM TestAttempt a WHERE a.test.id = :testId")
    long countDistinctUsersByTestId(@Param("testId") Long testId);

    @Query("SELECT AVG(a.score) FROM TestAttempt a WHERE a.test.id = :testId AND a.status = 'SUBMITTED'")
    Double findAverageScoreByTestId(@Param("testId") Long testId);

    @Query("SELECT MAX(a.score) FROM TestAttempt a WHERE a.test.id = :testId AND a.status = 'SUBMITTED'")
    Double findMaxScoreByTestId(@Param("testId") Long testId);

    @Query("SELECT MIN(a.score) FROM TestAttempt a WHERE a.test.id = :testId AND a.status = 'SUBMITTED'")
    Double findMinScoreByTestId(@Param("testId") Long testId);

    @Query("SELECT a FROM TestAttempt a WHERE a.test.id = :testId AND a.status = 'SUBMITTED' ORDER BY a.score DESC")
    List<TestAttempt> findLeaderboardByTestId(@Param("testId") Long testId);

    long countByTestId(Long testId);
}
