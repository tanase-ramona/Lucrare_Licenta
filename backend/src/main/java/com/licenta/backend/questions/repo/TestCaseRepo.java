package com.licenta.backend.questions.repo;

import com.licenta.backend.questions.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepo extends JpaRepository<TestCase, Long> {
    List<TestCase> findByQuestionIdOrderByOrderIndexAsc(Long questionId);

    boolean existsByQuestionId(Long questionId);
}
