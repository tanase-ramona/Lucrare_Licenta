package com.licenta.backend.interviews.core.repo;

import com.licenta.backend.interviews.core.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewQuestionRepo extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByInterviewIdOrderByOrderIndexAsc(Long interviewId);

    @Query("""
        select distinct iq from InterviewQuestion iq
        join fetch iq.question q
        join fetch q.category
        left join fetch q.languages
        left join fetch q.options
        where iq.interview.id = :interviewId
        order by iq.orderIndex
    """)
    List<InterviewQuestion> fetchDetails(@Param("interviewId") Long interviewId);
}