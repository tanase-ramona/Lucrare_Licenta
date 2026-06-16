package com.licenta.backend.interviews.filters.repo;

import com.licenta.backend.interviews.filters.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findByNameIgnoreCase(String name);
}
