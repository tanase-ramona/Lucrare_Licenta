package com.licenta.backend.questions.repo;

import com.licenta.backend.questions.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepo extends JpaRepository<Question, Long> {

    @Query("""
        select distinct q from Question q
        join q.languages l
        join q.positions p
        where q.active = true
          and q.category.id = :categoryId
          and q.level.id = :levelId
          and p.id = :positionId
          and l.id in :languageIds
    """)
    List<Question> findMatching(
            @Param("categoryId") Long categoryId,
            @Param("levelId") Long levelId,
            @Param("positionId") Long positionId,
            @Param("languageIds") List<Long> languageIds
    );
}