package com.mocktestapp.repository;

import com.mocktestapp.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByCreatedById(Long userId);

    List<Test> findByStatus(Test.TestStatus status);

    @Query("SELECT DISTINCT t FROM Test t LEFT JOIN t.assignments a WHERE (a.user.id = :userId AND a.isAllowed = true) OR (t.assignments IS EMPTY) OR (t.status = 'ACTIVE')")
    List<Test> findAssignedActiveTestsForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Test t WHERE t.createdBy.id = :adminId")
    long countByCreatedById(@Param("adminId") Long adminId);
}
