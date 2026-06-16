package com.licenta.backend.code.dto;

import java.util.List;

public class CodeRunResponseDto {
    public List<TestCaseResultDto> results;
    public int passed;
    public int total;

    public static class TestCaseResultDto {
        public Long testCaseId;
        public String description;
        public String inputData;
        public String expectedOutput;
        public String actualOutput;
        public boolean passed;
        public String error;
        public long executionTimeMs;
    }
}
