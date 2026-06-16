package com.licenta.backend.interviews.core.repo;

import com.licenta.backend.interviews.core.entity.AnswerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnswerFeedbackRepo extends JpaRepository<AnswerFeedback, Long> {

    Optional<AnswerFeedback> findByInterviewAnswerId(Long interviewAnswerId);

    List<AnswerFeedback> findByInterviewAnswerInterviewQuestionInterviewId(Long interviewId);

    @Query("SELECT l.name, AVG(f.score) FROM AnswerFeedback f JOIN f.interviewAnswer a JOIN a.interviewQuestion iq JOIN iq.question q JOIN q.languages l WHERE f.score IS NOT NULL GROUP BY l.name ORDER BY AVG(f.score) DESC")
    List<Object[]> findAvgScoreByLanguage();

    @Query("SELECT q.category.name, AVG(f.score) FROM AnswerFeedback f JOIN f.interviewAnswer a JOIN a.interviewQuestion iq JOIN iq.question q WHERE f.score IS NOT NULL GROUP BY q.category.name")
    List<Object[]> findAvgScoreByCategory();
}
