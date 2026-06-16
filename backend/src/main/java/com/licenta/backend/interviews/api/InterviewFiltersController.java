package com.licenta.backend.interviews.api;

import com.licenta.backend.interviews.api.dto.InterviewDetailsResponseDto;
import com.licenta.backend.interviews.api.dto.InterviewGenerateRequestDto;
import com.licenta.backend.interviews.api.dto.SubmitInterviewAnswersRequestDto;
import com.licenta.backend.interviews.core.service.InterviewGenerationService;
import com.licenta.backend.interviews.filters.repo.LanguageRepository;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.licenta.backend.interviews.api.dto.InterviewResultDto;
import com.licenta.backend.interviews.api.dto.InterviewHistoryItemDto;
import com.licenta.backend.interviews.api.dto.InterviewReviewResponseDto;
import com.licenta.backend.interviews.api.dto.RecommendationItemDto;
@RestController
@RequestMapping("/api/interview")
public class InterviewFiltersController {

    private final LevelRepository levelRepo;
    private final PositionRepository positionRepo;
    private final LanguageRepository languageRepo;

    private final InterviewGenerationService generationService; // ✅

    public InterviewFiltersController(LevelRepository levelRepo,
                                      PositionRepository positionRepo,
                                      LanguageRepository languageRepo,
                                      InterviewGenerationService generationService) { // ✅
        this.levelRepo = levelRepo;
        this.positionRepo = positionRepo;
        this.languageRepo = languageRepo;
        this.generationService = generationService; // ✅
    }

    @GetMapping("/filters")
    public FiltersResponseDto getFilters() {
        FiltersResponseDto dto = new FiltersResponseDto();
        dto.levels = levelRepo.findAll().stream()
                .map(x -> new FiltersResponseDto.Item(x.getId(), x.getName()))
                .toList();
        dto.positions = positionRepo.findAll().stream()
                .map(x -> new FiltersResponseDto.Item(x.getId(), x.getName()))
                .toList();
        dto.languages = languageRepo.findAll().stream()
                .map(x -> new FiltersResponseDto.Item(x.getId(), x.getName()))
                .toList();
        return dto;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody InterviewGenerateRequestDto req) {
        var interview = generationService.generate(req);
        return Map.of("interviewId", interview.getId());
    }

    @GetMapping("/{id}")
    @Transactional
    public InterviewDetailsResponseDto getInterview(@PathVariable Long id) {
        return generationService.getInterview(id);
    }

    @PostMapping("/{id}/answers")
    public java.util.Map<String, Object> submitAnswers(
            @PathVariable Long id,
            @RequestBody SubmitInterviewAnswersRequestDto req
    ) {
        generationService.submitAnswers(id, req);
        return java.util.Map.of("message", "Răspunsurile au fost salvate.");
    }

    @PostMapping("/{id}/finish")
    public InterviewResultDto finishInterview(@PathVariable Long id) {
        return generationService.finishInterview(id);
    }

    @GetMapping("/history")
    public java.util.List<InterviewHistoryItemDto> getHistory() {
        return generationService.getHistory();
    }

    @GetMapping("/{id}/review")
    public InterviewReviewResponseDto getInterviewReview(@PathVariable Long id) {
        return generationService.getInterviewReview(id);
    }

    @PostMapping("/{id}/analyze")
    public java.util.Map<String, Object> analyzeInterview(@PathVariable Long id) {
        generationService.analyzeInterviewAnswers(id);
        return java.util.Map.of("message", "Analiza AI a fost generată.");
    }

    @GetMapping("/{id}/result")
    public InterviewResultDto getInterviewResult(@PathVariable Long id) {
        return generationService.getInterviewResult(id);
    }

    @GetMapping("/recommendations")
    public java.util.List<RecommendationItemDto> getRecommendations() {
        return generationService.getRecommendations();
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String, Object> deleteInterview(@PathVariable Long id) {
        generationService.deleteInterview(id);
        return java.util.Map.of("message", "Interviul a fost șters.");
    }
}