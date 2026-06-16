package com.licenta.backend.interviews.core.repo;

import com.licenta.backend.interviews.core.entity.InterviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRequestRepo extends JpaRepository<InterviewRequest, Long> {}