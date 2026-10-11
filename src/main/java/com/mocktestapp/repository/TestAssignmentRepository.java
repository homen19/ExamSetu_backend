package com.mocktestapp.repository;

import com.mocktestapp.entity.TestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAssignmentRepository extends JpaRepository<TestAssignment, Long> {
    List<TestAssignment> findByTestId(Long testId);
    List<TestAssignment> findByUserId(Long userId);
    Optional<TestAssignment> findByTestIdAndUserId(Long testId, Long userId);
    boolean existsByTestIdAndUserId(Long testId, Long userId);
    long countByTestId(Long testId);
}
