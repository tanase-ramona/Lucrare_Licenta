package com.licenta.backend.admin.controller;

import com.licenta.backend.code.dto.TestCaseDto;
import com.licenta.backend.interviews.filters.entity.Language;
import com.licenta.backend.interviews.filters.entity.Level;
import com.licenta.backend.interviews.filters.entity.Position;
import com.licenta.backend.interviews.filters.repo.LanguageRepository;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import com.licenta.backend.questions.entity.Question;
import com.licenta.backend.questions.entity.QuestionCategory;
import com.licenta.backend.questions.entity.QuestionOption;
import com.licenta.backend.questions.entity.TestCase;
import com.licenta.backend.questions.repo.QuestionCategoryRepo;
import com.licenta.backend.questions.repo.QuestionRepo;
import com.licenta.backend.questions.repo.TestCaseRepo;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminTestCaseController {

    private final TestCaseRepo testCaseRepo;
    private final QuestionRepo questionRepo;
    private final QuestionCategoryRepo categoryRepo;
    private final LevelRepository levelRepo;
    private final LanguageRepository languageRepo;
    private final PositionRepository positionRepo;

    public AdminTestCaseController(TestCaseRepo testCaseRepo,
                                   QuestionRepo questionRepo,
                                   QuestionCategoryRepo categoryRepo,
                                   LevelRepository levelRepo,
                                   LanguageRepository languageRepo,
                                   PositionRepository positionRepo) {
        this.testCaseRepo = testCaseRepo;
        this.questionRepo = questionRepo;
        this.categoryRepo = categoryRepo;
        this.levelRepo = levelRepo;
        this.languageRepo = languageRepo;
        this.positionRepo = positionRepo;
    }

    public record QuestionSummaryDto(Long id, String text, String answerType, String category,
                                     List<String> languages) {}

    public record CreateQuestionDto(
            String text,
            String categoryName,
            Long levelId,
            List<Long> languageIds,
            List<Long> positionIds,
            String answerType,
            String starterCode,
            List<OptionDto> options
    ) {
        public record OptionDto(String text, boolean correct) {}
    }

    // Extended DTO with all fields needed for management UI
    public record QuestionDetailDto(
            Long id,
            String text,
            String answerType,
            String category,
            String level,
            List<String> languages,
            List<String> positions,
            boolean active,
            int testCaseCount
    ) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<QuestionDetailDto> listQuestions(
            @RequestParam(required = false) String answerType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean active
    ) {
        return questionRepo.findAll().stream()
                .filter(q -> answerType == null || q.getAnswerType().equalsIgnoreCase(answerType))
                .filter(q -> category  == null || q.getCategory().getName().equalsIgnoreCase(category))
                .filter(q -> level     == null || q.getLevel().getName().equalsIgnoreCase(level))
                .filter(q -> active    == null || q.isActive() == active)
                .filter(q -> language  == null || q.getLanguages().stream()
                        .anyMatch(l -> l.getName().equalsIgnoreCase(language)))
                .map(q -> new QuestionDetailDto(
                        q.getId(),
                        q.getText(),
                        q.getAnswerType(),
                        q.getCategory().getName(),
                        q.getLevel().getName(),
                        q.getLanguages().stream().map(Language::getName).sorted().toList(),
                        q.getPositions().stream().map(Position::getName).sorted().toList(),
                        q.isActive(),
                        (int) testCaseRepo.findByQuestionIdOrderByOrderIndexAsc(q.getId()).size()
                ))
                .sorted((a, b) -> a.category().compareTo(b.category()))
                .toList();
    }

    @PatchMapping("/{id}/active")
    @Transactional
    public Map<String, Object> toggleActive(@PathVariable Long id) {
        Question q = questionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        q.setActive(!q.isActive());
        questionRepo.save(q);
        return Map.of("id", id, "active", q.isActive());
    }

    @PutMapping("/{id}")
    @Transactional
    public QuestionDetailDto updateQuestion(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        Question q = questionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (body.containsKey("text")) {
            String text = (String) body.get("text");
            if (text != null && !text.isBlank()) q.setText(text.trim());
        }
        if (body.containsKey("active")) {
            q.setActive(Boolean.TRUE.equals(body.get("active")));
        }

        questionRepo.save(q);
        return new QuestionDetailDto(
                q.getId(), q.getText(), q.getAnswerType(),
                q.getCategory().getName(), q.getLevel().getName(),
                q.getLanguages().stream().map(Language::getName).sorted().toList(),
                q.getPositions().stream().map(Position::getName).sorted().toList(),
                q.isActive(),
                (int) testCaseRepo.findByQuestionIdOrderByOrderIndexAsc(q.getId()).size()
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> deleteQuestion(@PathVariable Long id) {
        if (!questionRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepo.deleteById(id);
        return Map.of("message", "Întrebarea a fost ștearsă.");
    }

    @PostMapping
    @Transactional
    public QuestionSummaryDto createQuestion(@RequestBody CreateQuestionDto dto) {
        if (dto.text() == null || dto.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Textul întrebării este obligatoriu.");
        }
        if (dto.answerType() == null || dto.categoryName() == null || dto.levelId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tip, categorie și nivel sunt obligatorii.");
        }

        QuestionCategory category = categoryRepo.findByName(dto.categoryName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categorie invalidă: " + dto.categoryName()));

        Level level = levelRepo.findById(dto.levelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel invalid."));

        Set<Language> languages = new HashSet<>();
        if (dto.languageIds() != null) {
            for (Long id : dto.languageIds()) {
                languageRepo.findById(id).ifPresent(languages::add);
            }
        }

        Set<Position> positions = new HashSet<>();
        if (dto.positionIds() != null) {
            for (Long id : dto.positionIds()) {
                positionRepo.findById(id).ifPresent(positions::add);
            }
        }

        Question q = new Question();
        q.setText(dto.text());
        q.setCategory(category);
        q.setLevel(level);
        q.setLanguages(languages);
        q.setPositions(positions);
        q.setAnswerType(dto.answerType());
        q.setStarterCode(dto.starterCode());
        q.setActive(true);

        if ("MCQ".equalsIgnoreCase(dto.answerType()) && dto.options() != null) {
            for (CreateQuestionDto.OptionDto opt : dto.options()) {
                if (opt.text() != null && !opt.text().isBlank()) {
                    q.getOptions().add(new QuestionOption(q, opt.text(), opt.correct()));
                }
            }
        }

        Question saved = questionRepo.save(q);

        return new QuestionSummaryDto(
                saved.getId(),
                saved.getText(),
                saved.getAnswerType(),
                saved.getCategory().getName(),
                saved.getLanguages().stream().map(Language::getName).collect(Collectors.toList())
        );
    }

    @GetMapping("/{questionId}/testcases")
    public List<TestCaseDto> getTestCases(@PathVariable Long questionId) {
        return testCaseRepo.findByQuestionIdOrderByOrderIndexAsc(questionId)
                .stream().map(this::toDto).toList();
    }

    @PostMapping("/{questionId}/testcases")
    public TestCaseDto addTestCase(@PathVariable Long questionId, @RequestBody TestCaseDto dto) {
        Question question = questionRepo.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        TestCase tc = new TestCase();
        tc.setQuestion(question);
        tc.setDescription(dto.description);
        tc.setInputData(dto.inputData);
        tc.setExpectedOutput(dto.expectedOutput);
        tc.setOrderIndex(dto.orderIndex != null ? dto.orderIndex : 0);

        return toDto(testCaseRepo.save(tc));
    }

    @DeleteMapping("/testcases/{id}")
    public Map<String, String> deleteTestCase(@PathVariable Long id) {
        if (!testCaseRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case not found");
        }
        testCaseRepo.deleteById(id);
        return Map.of("message", "Test case deleted");
    }

    private TestCaseDto toDto(TestCase tc) {
        TestCaseDto dto = new TestCaseDto();
        dto.id = tc.getId();
        dto.description = tc.getDescription();
        dto.inputData = tc.getInputData();
        dto.expectedOutput = tc.getExpectedOutput();
        dto.orderIndex = tc.getOrderIndex();
        return dto;
    }
}
