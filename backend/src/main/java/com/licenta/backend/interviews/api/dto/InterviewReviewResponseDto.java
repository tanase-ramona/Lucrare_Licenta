package com.licenta.backend.interviews.api.dto;

import java.util.List;

public class InterviewReviewResponseDto {
    public Long interviewId;
    public String status;
    public Integer score;

    public FeedbackSummaryDto feedbackSummary;
    public List<QuestionReviewItem> questions;
    public Boolean correct;

    public static class FeedbackSummaryDto {
        public String readyLevel;
        public String summaryText;
        public String strongPoints;
        public String weakPoints;
        public String recommendedTopics;
        public String recommendedProblemCategories;
        public String nextSteps;
    }

    public static class QuestionReviewItem {
        public Long interviewQuestionId;
        public String text;
        public String category;
        public String answerType;
        public List<OptionItem> options;
        public List<String> languages;

        public String answerText;
        public Long selectedOptionId;

        public Integer aiScore;
        public Boolean aiGood;
        public String aiStrengths;
        public String aiWeaknesses;
        public String aiImprovementTips;
        public String aiSuggestedAnswer;
        public String aiFeedbackJson;
    }

    public static class OptionItem {
        public Long id;
        public String text;
        public Boolean correct;
    }
}