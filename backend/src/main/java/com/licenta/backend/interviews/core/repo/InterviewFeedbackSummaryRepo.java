package com.licenta.backend.interviews.core.repo;

import com.licenta.backend.interviews.core.entity.InterviewFeedbackSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewFeedbackSummaryRepo extends JpaRepository<InterviewFeedbackSummary, Long> {
    Optional<InterviewFeedbackSummary> findByInterviewId(Long interviewId);

    @Query("SELECT s FROM InterviewFeedbackSummary s WHERE s.interview.user.id = :userId ORDER BY s.interview.createdAt DESC")
    List<InterviewFeedbackSummary> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}