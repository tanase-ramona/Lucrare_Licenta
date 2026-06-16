package com.licenta.backend.interviews.api.dto;

import java.time.Instant;
import java.util.List;

public class InterviewHistoryItemDto {
    public Long interviewId;
    public String status;
    public Integer score;
    public Instant createdAt;
    public String level;
    public String position;
    public String readyLevel;
    // IDs pentru regenerare interviu cu aceleași setări
    public Long levelId;
    public Long positionId;
    public List<Long> languageIds;
}