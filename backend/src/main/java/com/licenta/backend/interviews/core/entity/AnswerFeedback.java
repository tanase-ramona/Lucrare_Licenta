package com.licenta.backend.interviews.core.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "answer_feedback")
public class AnswerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id", unique = true)
    private InterviewAnswer interviewAnswer;

    @Column(name = "score")
    private Integer score;

    @Column(name = "is_good")
    private Boolean good;

    @Column(name = "strengths", length = 5000)
    private String strengths;

    @Column(name = "weaknesses", length = 5000)
    private String weaknesses;

    @Column(name = "improvement_tips", length = 5000)
    private String improvementTips;

    @Column(name = "suggested_answer", length = 10000)
    private String suggestedAnswer;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public AnswerFeedback() {}

    public Long getId() { return id; }
    public InterviewAnswer getInterviewAnswer() { return interviewAnswer; }
    public Integer getScore() { return score; }
    public Boolean getGood() { return good; }
    public String getStrengths() { return strengths; }
    public String getWeaknesses() { return weaknesses; }
    public String getImprovementTips() { return improvementTips; }
    public String getSuggestedAnswer() { return suggestedAnswer; }
    public String getFeedbackJson() { return feedbackJson; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setInterviewAnswer(InterviewAnswer interviewAnswer) { this.interviewAnswer = interviewAnswer; }
    public void setScore(Integer score) { this.score = score; }
    public void setGood(Boolean good) { this.good = good; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public void setImprovementTips(String improvementTips) { this.improvementTips = improvementTips; }
    public void setSuggestedAnswer(String suggestedAnswer) { this.suggestedAnswer = suggestedAnswer; }
    public void setFeedbackJson(String feedbackJson) { this.feedbackJson = feedbackJson; }
}