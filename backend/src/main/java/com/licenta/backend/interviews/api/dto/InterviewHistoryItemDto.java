package com.licenta.backend.interviews.api.dto;

import java.time.Instant;

public class InterviewHistoryItemDto {
    public Long interviewId;
    public String status;
    public Integer score;
    public Instant createdAt;
    public String level;
    public String position;
    public String readyLevel;
}