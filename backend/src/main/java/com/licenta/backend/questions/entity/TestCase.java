package com.licenta.backend.questions.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private String description;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    public TestCase() {}

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getDescription() { return description; }
    public String getInputData() { return inputData; }
    public String getExpectedOutput() { return expectedOutput; }
    public Integer getOrderIndex() { return orderIndex; }

    public void setId(Long id) { this.id = id; }
    public void setQuestion(Question question) { this.question = question; }
    public void setDescription(String description) { this.description = description; }
    public void setInputData(String inputData) { this.inputData = inputData; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
