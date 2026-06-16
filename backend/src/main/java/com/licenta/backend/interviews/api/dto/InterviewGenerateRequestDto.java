package com.licenta.backend.interviews.api.dto;

import java.util.List;

public class InterviewGenerateRequestDto {
    public Long levelId;
    public Long positionId;
    public List<Long> languageIds;

    // Opționale — dacă lipsesc, se folosesc valorile implicite (2 HR, 4 TECH, 2 PROBLEM)
    public Integer hrCount;
    public Integer techCount;
    public Integer codingCount;
}