package com.licenta.backend.interviews.core.entity;

import com.licenta.backend.questions.entity.Question;
import jakarta.persistence.*;

@Entity
@Table(
        name = "interview_questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"interview_id", "order_index"})
)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public InterviewQuestion() {}

    public Long getId() { return id; }
    public Interview getInterview() { return interview; }
    public Question getQuestion() { return question; }
    public int getOrderIndex() { return orderIndex; }

    public void setId(Long id) { this.id = id; }
    public void setInterview(Interview interview) { this.interview = interview; }
    public void setQuestion(Question question) { this.question = question; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}