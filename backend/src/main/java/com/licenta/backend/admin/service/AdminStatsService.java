package com.licenta.backend.admin.service;

import com.licenta.backend.admin.dto.AdminStatsDto;
import com.licenta.backend.interviews.core.repo.AnswerFeedbackRepo;
import com.licenta.backend.interviews.core.repo.InterviewRepo;
import com.licenta.backend.users.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    private final InterviewRepo interviewRepo;
    private final AnswerFeedbackRepo answerFeedbackRepo;
    private final UserRepo userRepo;

    public AdminStatsService(InterviewRepo interviewRepo,
                             AnswerFeedbackRepo answerFeedbackRepo,
                             UserRepo userRepo) {
        this.interviewRepo = interviewRepo;
        this.answerFeedbackRepo = answerFeedbackRepo;
        this.userRepo = userRepo;
    }

    public AdminStatsDto getStats(String period) {
        AdminStatsDto dto = new AdminStatsDto();

        long days = switch (period) {
            case "6m" -> 180L;
            case "1y" -> 365L;
            default  ->  30L;
        };
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        // ── Overview ──────────────────────────────────────────────────────────
        AdminStatsDto.OverviewDto overview = new AdminStatsDto.OverviewDto();
        overview.totalUsers          = userRepo.countNonAdminUsers();
        overview.totalInterviews     = interviewRepo.count();
        overview.completedInterviews = interviewRepo.countByStatus("COMPLETED");
        overview.avgScore            = roundOne(interviewRepo.findAvgScore());
        overview.activeUsersInPeriod = interviewRepo.countDistinctNonAdminUsersSince(since);
        overview.interviewsInPeriod  = interviewRepo.countSince(since);
        overview.completedInPeriod   = interviewRepo.countCompletedSince(since);
        dto.overview = overview;

        // ── Position stats ────────────────────────────────────────────────────
        dto.positionStats = toNameValue(interviewRepo.findPositionStats(), 0, 1);

        // ── Level distribution ────────────────────────────────────────────────
        dto.levelDistribution = toNameValue(interviewRepo.findChosenLevelDistribution(), 0, 1);

        // ── Language performance ──────────────────────────────────────────────
        dto.languageScores = toNameValueDouble(answerFeedbackRepo.findAvgScoreByLanguage(), 0, 1);

        // ── Category performance ──────────────────────────────────────────────
        dto.categoryPerformance = toNameValueDouble(answerFeedbackRepo.findAvgScoreByCategory(), 0, 1);

        // ── Interviews over time (period) ─────────────────────────────────────
        List<Object[]> daily = interviewRepo.findDailyInterviewCounts(since);
        List<AdminStatsDto.DayCountDto> timeline = new ArrayList<>();
        for (Object[] row : daily) {
            String date  = row[0] != null ? row[0].toString() : "?";
            long   count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            timeline.add(new AdminStatsDto.DayCountDto(date, count));
        }
        dto.interviewsOverTime = timeline;

        // ── Avg score evolution (period) ──────────────────────────────────────
        List<Object[]> avgScoreDaily = interviewRepo.findDailyAvgScore(since);
        List<AdminStatsDto.DayScoreDto> scoreTimeline = new ArrayList<>();
        for (Object[] row : avgScoreDaily) {
            String date  = row[0] != null ? row[0].toString() : "?";
            double score = row[1] != null ? roundOne(((Number) row[1]).doubleValue()) : 0;
            scoreTimeline.add(new AdminStatsDto.DayScoreDto(date, score));
        }
        dto.avgScoreOverTime = scoreTimeline;

        // ── Success rate ──────────────────────────────────────────────────────
        long completed  = overview.completedInterviews;
        long successful = interviewRepo.countSuccessfulInterviews();
        dto.successRate = completed == 0 ? 0 : roundOne((successful * 100.0) / completed);

        return dto;
    }

    private List<AdminStatsDto.NameValueDto> toNameValue(List<Object[]> rows, int nameIdx, int valueIdx) {
        List<AdminStatsDto.NameValueDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            String name  = row[nameIdx] != null ? row[nameIdx].toString() : "?";
            double value = row[valueIdx] != null ? ((Number) row[valueIdx]).doubleValue() : 0;
            list.add(new AdminStatsDto.NameValueDto(name, value));
        }
        return list;
    }

    private List<AdminStatsDto.NameValueDto> toNameValueDouble(List<Object[]> rows, int nameIdx, int valueIdx) {
        List<AdminStatsDto.NameValueDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            String name  = row[nameIdx] != null ? row[nameIdx].toString() : "?";
            double value = row[valueIdx] != null ? roundOne(((Number) row[valueIdx]).doubleValue()) : 0;
            list.add(new AdminStatsDto.NameValueDto(name, value));
        }
        return list;
    }

    private double roundOne(Double d) {
        if (d == null) return 0;
        return Math.round(d * 10.0) / 10.0;
    }
}
