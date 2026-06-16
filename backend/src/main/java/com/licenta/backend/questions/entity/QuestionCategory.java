package com.licenta.backend.questions.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_categories")
public class QuestionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // HR / TECH / PROBLEM

    public QuestionCategory() {}
    public QuestionCategory(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}