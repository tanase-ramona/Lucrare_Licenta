package com.licenta.backend.questions.repo;

import com.licenta.backend.questions.entity.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionCategoryRepo extends JpaRepository<QuestionCategory, Long> {
    Optional<QuestionCategory> findByName(String name);
}