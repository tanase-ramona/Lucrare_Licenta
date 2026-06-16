package com.licenta.backend.questions.repo;

import com.licenta.backend.questions.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepo extends JpaRepository<QuestionOption, Long> {}