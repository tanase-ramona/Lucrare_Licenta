package com.licenta.backend.users.entity;

import com.licenta.backend.interviews.filters.entity.Level;
import com.licenta.backend.interviews.filters.entity.Position;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName = "";

    @Column(name = "last_name")
    private String lastName = "";

    // stocat BCrypt
    @Column(name = "password_hash", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_level_id")
    private Level profileLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_position_id")
    private Position profilePosition;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public User() {}

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPassword() { return password; }
    public Level getProfileLevel() { return profileLevel; }
    public Position getProfilePosition() { return profilePosition; }
    public Set<Role> getRoles() { return roles; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPassword(String password) { this.password = password; }
    public void setProfileLevel(Level profileLevel) { this.profileLevel = profileLevel; }
    public void setProfilePosition(Position profilePosition) { this.profilePosition = profilePosition; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
