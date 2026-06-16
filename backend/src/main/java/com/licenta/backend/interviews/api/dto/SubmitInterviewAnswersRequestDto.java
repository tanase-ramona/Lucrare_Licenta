package com.licenta.backend.interviews.api.dto;

import java.util.List;

public class SubmitInterviewAnswersRequestDto {

    public List<AnswerItem> answers;

    public static class AnswerItem {
        public Long interviewQuestionId;
        public String answerText;
        public Long selectedOptionId;
    }
}