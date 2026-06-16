package com.licenta.backend.interviews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.backend.interviews.filters.repo.LanguageRepository;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewGenerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private LevelRepository levelRepo;

    @Autowired
    private PositionRepository positionRepo;

    @Autowired
    private LanguageRepository languageRepo;

    @Test
    void generateInterviewProducesExpectedQuestionStructure() throws Exception {
        Long levelId = levelRepo.findAll().stream()
                .filter(l -> "Junior".equalsIgnoreCase(l.getName()))
                .findFirst().orElseThrow().getId();
        Long positionId = positionRepo.findAll().stream()
                .filter(p -> "Backend".equalsIgnoreCase(p.getName()))
                .findFirst().orElseThrow().getId();
        Long javaId = languageRepo.findByNameIgnoreCase("Java").orElseThrow().getId();

        Map<String, Object> registerPayload = Map.of(
                "email", "interview.test@example.com",
                "password", "password123",
                "confirmPassword", "password123",
                "firstName", "Test",
                "lastName", "User",
                "levelId", levelId,
                "positionId", positionId
        );

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("accessToken").asText();

        Map<String, Object> generatePayload = Map.of(
                "levelId", levelId,
                "positionId", positionId,
                "languageIds", List.of(javaId)
        );

        String generateResponse = mockMvc.perform(post("/api/interview/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generatePayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long interviewId = objectMapper.readTree(generateResponse).get("interviewId").asLong();

        String detailsResponse = mockMvc.perform(get("/api/interview/" + interviewId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode questions = objectMapper.readTree(detailsResponse).get("questions");
        assertThat(questions).hasSize(8);

        Map<String, Long> countsByCategory = StreamSupport.stream(questions.spliterator(), false)
                .collect(Collectors.groupingBy(q -> q.get("category").asText(), Collectors.counting()));

        assertThat(countsByCategory.get("HR")).isEqualTo(2L);
        assertThat(countsByCategory.get("TECH")).isEqualTo(4L);
        assertThat(countsByCategory.get("PROBLEM")).isEqualTo(2L);
    }
}
