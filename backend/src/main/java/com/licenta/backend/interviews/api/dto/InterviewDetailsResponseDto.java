package com.licenta.backend.interviews.api.dto;

import java.util.List;

public class InterviewDetailsResponseDto {

    public Long id;
    public String status;
    public List<QuestionItem> questions;

    public static class QuestionItem {
        public Long interviewQuestionId;
        public Long questionId;
        public String text;
        public String category;
        public String answerType;
        public String starterCode;
        public List<OptionItem> options;
        public List<String> languages;
    }

    public static class OptionItem {
        public Long id;
        public String text;
    }
}
