package com.licenta.backend.interviews.core.repo;

import com.licenta.backend.interviews.core.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewAnswerRepo extends JpaRepository<InterviewAnswer, Long> {
    List<InterviewAnswer> findByInterviewQuestionInterviewId(Long interviewId);
    Optional<InterviewAnswer> findByInterviewQuestionId(Long interviewQuestionId);
}