package com.mocktestapp.repository;

import com.mocktestapp.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTestIdOrderByQuestionOrder(Long testId);
    long countByTestId(Long testId);
}
