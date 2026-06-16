package com.licenta.backend.interviews.filters.repo;

import com.licenta.backend.interviews.filters.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByNameIgnoreCase(String name);
}