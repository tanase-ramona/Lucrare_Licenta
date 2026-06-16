package com.licenta.backend.questions.entity;

import jakarta.persistence.*;
@Entity
@Table(name = "question_options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    public QuestionOption() {}

    public QuestionOption(Question question, String text, boolean correct) {
        this.question = question;
        this.text = text;
        this.correct = correct;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }

    public void setId(Long id) { this.id = id; }
    public void setQuestion(Question question) { this.question = question; }
    public void setText(String text) { this.text = text; }
    public void setCorrect(boolean correct) { this.correct = correct; }

}