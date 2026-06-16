package com.licenta.backend.users.repo;

import com.licenta.backend.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countCreatedSince(@Param("since") Instant since);

    @Query("SELECT COUNT(u) FROM User u WHERE NOT EXISTS (SELECT r FROM u.roles r WHERE r.name = 'ADMIN')")
    long countNonAdminUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since AND NOT EXISTS (SELECT r FROM u.roles r WHERE r.name = 'ADMIN')")
    long countNonAdminUsersCreatedSince(@Param("since") Instant since);
}
