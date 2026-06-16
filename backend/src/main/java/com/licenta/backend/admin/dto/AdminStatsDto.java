package com.licenta.backend.admin.dto;

import java.util.List;

public class AdminStatsDto {

    public OverviewDto overview;
    public List<NameValueDto> positionStats;
    public List<NameValueDto> levelDistribution;
    public List<NameValueDto> languageScores;
    public List<NameValueDto> categoryPerformance;
    public List<DayCountDto> interviewsOverTime;
    public List<DayScoreDto> avgScoreOverTime;
    public double successRate;

    public static class OverviewDto {
        public long totalUsers;
        public long totalInterviews;
        public long completedInterviews;
        public double avgScore;
        public long activeUsersInPeriod;
        public long interviewsInPeriod;
        public long completedInPeriod;
    }

    public static class NameValueDto {
        public String name;
        public double value;

        public NameValueDto(String name, double value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class DayCountDto {
        public String date;
        public long count;

        public DayCountDto(String date, long count) {
            this.date = date;
            this.count = count;
        }
    }

    public static class DayScoreDto {
        public String date;
        public double score;

        public DayScoreDto(String date, double score) {
            this.date = date;
            this.score = score;
        }
    }
}
