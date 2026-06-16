package com.licenta.backend.interviews.api.dto;

public class InterviewResultDto {
    public Long interviewId;
    public String status;
    public int totalMcq;
    public int correctMcq;
    public int score;

    public String readyLevel;
    public String summaryText;
    public String strongPoints;
    public String weakPoints;
    public String recommendedTopics;
    public String recommendedProblemCategories;
    public String nextSteps;
}