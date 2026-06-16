package com.licenta.backend.interviews.core.entity;

import com.licenta.backend.questions.entity.QuestionOption;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "interview_answers")
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id")
    private InterviewQuestion interviewQuestion;

    @Column(name = "answer_text", length = 10000)
    private String answerText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public InterviewAnswer() {}

    public Long getId() { return id; }
    public InterviewQuestion getInterviewQuestion() { return interviewQuestion; }
    public String getAnswerText() { return answerText; }
    public QuestionOption getSelectedOption() { return selectedOption; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setInterviewQuestion(InterviewQuestion interviewQuestion) { this.interviewQuestion = interviewQuestion; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public void setSelectedOption(QuestionOption selectedOption) { this.selectedOption = selectedOption; }
}