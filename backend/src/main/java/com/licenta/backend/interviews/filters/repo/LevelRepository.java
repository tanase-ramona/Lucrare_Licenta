package com.licenta.backend.interviews.filters.repo;

import com.licenta.backend.interviews.filters.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Long> {}