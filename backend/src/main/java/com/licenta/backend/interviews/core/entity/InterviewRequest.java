package com.licenta.backend.interviews.core.entity;

import com.licenta.backend.interviews.filters.entity.Language;
import com.licenta.backend.interviews.filters.entity.Level;
import com.licenta.backend.interviews.filters.entity.Position;
import com.licenta.backend.users.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "interview_requests")
public class InterviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "level_id")
    private Level level;

    @ManyToOne(optional = false)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToMany
    @JoinTable(
            name = "interview_request_languages",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    private Set<Language> languages = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public InterviewRequest() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Level getLevel() { return level; }
    public Position getPosition() { return position; }
    public Set<Language> getLanguages() { return languages; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setLevel(Level level) { this.level = level; }
    public void setPosition(Position position) { this.position = position; }
    public void setLanguages(Set<Language> languages) { this.languages = languages; }
}