package com.licenta.backend.interviews.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class GroqApiClient {

    private static final Logger log = LoggerFactory.getLogger(GroqApiClient.class);

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${groq.max-tokens:8192}")
    private int maxTokens;

    private final RestTemplate restTemplate = createRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRIES = 4;
    private static final long INITIAL_BACKOFF_MS = 5_000;

    public String generate(String prompt) {
        log.debug("Groq generate called. model={}, keyPresent={}", model, apiKey != null && !apiKey.isBlank());

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Groq API key not configured (groq.api.key)");
        }

        long backoff = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return doGenerate(prompt);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 && attempt < MAX_RETRIES) {
                    String body = e.getResponseBodyAsString();
                    if (isDailyTokenLimit(body)) {
                        log.warn("Groq daily token limit reached. body={}", body);
                        throw new RuntimeException("Groq daily token limit reached", e);
                    }
                    log.warn("Groq rate-limited (429), attempt {}/{}. Retrying in {}ms...", attempt, MAX_RETRIES, backoff);
                    sleep(backoff);
                    backoff *= 2;
                } else {
                    log.error("Groq HTTP client error {} - body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                    throw new RuntimeException("Groq API client error: " + e.getStatusCode(), e);
                }
            } catch (HttpServerErrorException e) {
                log.error("Groq HTTP server error {} - body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("Groq API server error: " + e.getStatusCode(), e);
            } catch (Exception e) {
                log.error("Groq unexpected error: {}", e.getMessage(), e);
                throw new RuntimeException("Groq API error: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Groq API rate limit exceeded after " + MAX_RETRIES + " retries");
    }

    private boolean isDailyTokenLimit(String body) {
        return body != null
                && (body.contains("tokens per day")
                || body.contains("\"type\":\"tokens\"")
                || body.contains("TPD"));
    }

    private String doGenerate(String prompt) throws Exception {
        String requestBody = buildRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                GROQ_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        String responseBody = response.getBody();
        log.debug("Groq raw response: {}", responseBody);

        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.at("/choices/0/message/content").asText("");

        if (text.isBlank()) {
            log.error("Groq returned empty text. Full response: {}", responseBody);
            throw new RuntimeException("Groq returned empty response");
        }

        log.debug("Groq response text: {}", text);
        return text;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));
        return new RestTemplate(factory);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        root.put("max_tokens", maxTokens);

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        var messages = root.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Returneaza exclusiv JSON valid, fara markdown si fara text in afara obiectului JSON.");

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        return objectMapper.writeValueAsString(root);
    }
}
