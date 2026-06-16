package com.licenta.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private LevelRepository levelRepo;

    @Autowired
    private PositionRepository positionRepo;

    private Map<String, Object> registerPayload(String email, String password) {
        Long levelId = levelRepo.findAll().stream()
                .filter(l -> "Junior".equalsIgnoreCase(l.getName()))
                .findFirst().orElseThrow().getId();
        Long positionId = positionRepo.findAll().stream()
                .filter(p -> "Backend".equalsIgnoreCase(p.getName()))
                .findFirst().orElseThrow().getId();

        return Map.of(
                "email", email,
                "password", password,
                "confirmPassword", password,
                "firstName", "Test",
                "lastName", "User",
                "levelId", levelId,
                "positionId", positionId
        );
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() throws Exception {
        Map<String, Object> payload = registerPayload("duplicate.user@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        Map<String, Object> payload = registerPayload("login.test@example.com", "correctpass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        Map<String, Object> loginPayload = Map.of(
                "email", "login.test@example.com",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isUnauthorized());
    }
}
