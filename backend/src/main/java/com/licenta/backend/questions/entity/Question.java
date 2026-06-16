package com.licenta.backend.questions.entity;

import com.licenta.backend.interviews.filters.entity.Language;
import com.licenta.backend.interviews.filters.entity.Level;
import com.licenta.backend.interviews.filters.entity.Position;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private QuestionCategory category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "level_id")
    private Level level;

    @Column(nullable = false, length = 4000)
    private String text;

    @ManyToMany
    @JoinTable(
            name = "question_languages",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    private Set<Language> languages = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "question_positions",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "position_id")
    )
    private Set<Position> positions = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "answer_type", nullable = false)
    private String answerType;
    // "TEXT" (HR), "MCQ" (TECH), "CODE" (PROBLEM)

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<QuestionOption> options = new java.util.ArrayList<>();

    public String getAnswerType() { return answerType; }
    public void setAnswerType(String answerType) { this.answerType = answerType; }
    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }

    public java.util.List<QuestionOption> getOptions() { return options; }
    public void setOptions(java.util.List<QuestionOption> options) { this.options = options; }

    public Question() {}

    public Long getId() { return id; }
    public QuestionCategory getCategory() { return category; }
    public Level getLevel() { return level; }
    public String getText() { return text; }
    public Set<Language> getLanguages() { return languages; }
    public Set<Position> getPositions() { return positions; }
    public boolean isActive() { return active; }

    public void setId(Long id) { this.id = id; }
    public void setCategory(QuestionCategory category) { this.category = category; }
    public void setLevel(Level level) { this.level = level; }
    public void setText(String text) { this.text = text; }
    public void setLanguages(Set<Language> languages) { this.languages = languages; }
    public void setPositions(Set<Position> positions) { this.positions = positions; }
    public void setActive(boolean active) { this.active = active; }
}
