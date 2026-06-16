package com.licenta.backend.interviews.core.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "interview_feedback_summary")
public class InterviewFeedbackSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", unique = true)
    private Interview interview;

    @Column(name = "ready_level", length = 100)
    private String readyLevel; // READY / PARTIALLY_READY / NOT_READY

    @Column(name = "summary_text", length = 10000)
    private String summaryText;

    @Column(name = "strong_points", length = 10000)
    private String strongPoints;

    @Column(name = "weak_points", length = 10000)
    private String weakPoints;

    @Column(name = "recommended_topics", length = 10000)
    private String recommendedTopics;

    @Column(name = "recommended_problem_categories", length = 10000)
    private String recommendedProblemCategories;

    @Column(name = "next_steps", length = 10000)
    private String nextSteps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public InterviewFeedbackSummary() {}

    public Long getId() { return id; }
    public Interview getInterview() { return interview; }
    public String getReadyLevel() { return readyLevel; }
    public String getSummaryText() { return summaryText; }
    public String getStrongPoints() { return strongPoints; }
    public String getWeakPoints() { return weakPoints; }
    public String getRecommendedTopics() { return recommendedTopics; }
    public String getRecommendedProblemCategories() { return recommendedProblemCategories; }
    public String getNextSteps() { return nextSteps; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setInterview(Interview interview) { this.interview = interview; }
    public void setReadyLevel(String readyLevel) { this.readyLevel = readyLevel; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public void setStrongPoints(String strongPoints) { this.strongPoints = strongPoints; }
    public void setWeakPoints(String weakPoints) { this.weakPoints = weakPoints; }
    public void setRecommendedTopics(String recommendedTopics) { this.recommendedTopics = recommendedTopics; }
    public void setRecommendedProblemCategories(String recommendedProblemCategories) { this.recommendedProblemCategories = recommendedProblemCategories; }
    public void setNextSteps(String nextSteps) { this.nextSteps = nextSteps; }
}