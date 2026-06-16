package com.licenta.backend.interviews.core.entity;

import com.licenta.backend.users.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private InterviewRequest request;

    @Column(nullable = false)
    private String status; // GENERATED / IN_PROGRESS / COMPLETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "score")
    private Integer score;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public Interview() {}

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public InterviewRequest getRequest() { return request; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setRequest(InterviewRequest request) { this.request = request; }
    public void setStatus(String status) { this.status = status; }
}