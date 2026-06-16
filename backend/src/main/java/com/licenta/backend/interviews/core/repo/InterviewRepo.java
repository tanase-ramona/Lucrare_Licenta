package com.licenta.backend.interviews.core.repo;

import com.licenta.backend.interviews.core.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InterviewRepo extends JpaRepository<Interview, Long> {

    List<Interview> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(String status);

    @Query("SELECT COUNT(DISTINCT i.user.id) FROM Interview i WHERE i.createdAt >= :since")
    long countDistinctUsersSince(@Param("since") Instant since);

    @Query("SELECT COUNT(DISTINCT i.user.id) FROM Interview i WHERE i.createdAt >= :since AND NOT EXISTS (SELECT r FROM i.user.roles r WHERE r.name = 'ADMIN')")
    long countDistinctNonAdminUsersSince(@Param("since") Instant since);

    @Query("SELECT AVG(i.score) FROM Interview i WHERE i.status = 'COMPLETED' AND i.score IS NOT NULL")
    Double findAvgScore();

    @Query("SELECT i.request.position.name, COUNT(i), COALESCE(AVG(i.score), 0) FROM Interview i WHERE i.status = 'COMPLETED' GROUP BY i.request.position.name ORDER BY COUNT(i) DESC")
    List<Object[]> findPositionStats();

    @Query("SELECT i.score FROM Interview i WHERE i.status = 'COMPLETED' AND i.score IS NOT NULL")
    List<Integer> findAllCompletedScores();

    @Query("SELECT i.request.level.name, COUNT(i) FROM Interview i GROUP BY i.request.level.name ORDER BY COUNT(i) DESC")
    List<Object[]> findChosenLevelDistribution();

    @Query("SELECT FUNCTION('DATE', i.createdAt), COUNT(i) FROM Interview i WHERE i.createdAt >= :since GROUP BY FUNCTION('DATE', i.createdAt) ORDER BY FUNCTION('DATE', i.createdAt)")
    List<Object[]> findDailyInterviewCounts(@Param("since") Instant since);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.status = 'COMPLETED' AND i.score >= 70")
    long countSuccessfulInterviews();

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.createdAt >= :since")
    long countSince(@Param("since") Instant since);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.status = 'COMPLETED' AND i.createdAt >= :since")
    long countCompletedSince(@Param("since") Instant since);

    @Query("SELECT FUNCTION('DATE', i.createdAt), AVG(i.score) FROM Interview i WHERE i.status = 'COMPLETED' AND i.score IS NOT NULL AND i.createdAt >= :since GROUP BY FUNCTION('DATE', i.createdAt) ORDER BY FUNCTION('DATE', i.createdAt)")
    List<Object[]> findDailyAvgScore(@Param("since") Instant since);
}
