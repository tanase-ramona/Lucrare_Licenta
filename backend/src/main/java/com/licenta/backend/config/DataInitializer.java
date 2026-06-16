package com.licenta.backend.config;

import com.licenta.backend.interviews.filters.entity.Language;
import com.licenta.backend.interviews.filters.entity.Level;
import com.licenta.backend.interviews.filters.entity.Position;
import com.licenta.backend.interviews.filters.repo.LanguageRepository;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(3)
public class DataInitializer implements CommandLineRunner {

    private final LevelRepository levelRepo;
    private final LanguageRepository languageRepo;
    private final PositionRepository positionRepo;

    public DataInitializer(LevelRepository levelRepo,
                           LanguageRepository languageRepo,
                           PositionRepository positionRepo) {
        this.levelRepo = levelRepo;
        this.languageRepo = languageRepo;
        this.positionRepo = positionRepo;
    }

    @Override
    public void run(String... args) {
        ensureLevels("Intern", "Junior", "Mid", "Senior");
        ensureLanguages("Java", "C", "C++", "Python", "JavaScript", "TypeScript", "NodeJS");
        ensurePositions("Backend", "Frontend", "Fullstack", "QA", "Student", "Graduate");
    }

    private void ensureLevels(String... names) {
        var existing = levelRepo.findAll().stream()
                .map(level -> level.getName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        for (String name : names) {
            if (!existing.contains(name.toLowerCase())) {
                levelRepo.save(new Level(name));
            }
        }
    }

    private void ensureLanguages(String... names) {
        for (String name : names) {
            languageRepo.findByNameIgnoreCase(name)
                    .orElseGet(() -> languageRepo.save(new Language(name)));
        }
    }

    private void ensurePositions(String... names) {
        for (String name : names) {
            positionRepo.findByNameIgnoreCase(name)
                    .orElseGet(() -> positionRepo.save(new Position(name)));
        }
    }
}
