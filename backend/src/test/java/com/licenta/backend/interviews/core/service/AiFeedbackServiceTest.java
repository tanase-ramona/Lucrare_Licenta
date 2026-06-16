package com.licenta.backend.interviews.core.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiFeedbackServiceTest {

    @ParameterizedTest
    @CsvSource({
            "100, READY",
            "75,  READY",
            "74,  PARTIALLY_READY",
            "45,  PARTIALLY_READY",
            "44,  NOT_READY",
            "0,   NOT_READY"
    })
    void readyLevelFallbackFollowsMcqScoreThresholds(int mcqScore, String expectedReadyLevel) {
        GroqApiClient groqClient = mock(GroqApiClient.class);
        when(groqClient.generate(any())).thenThrow(new RuntimeException("Groq unavailable"));

        AiFeedbackService service = new AiFeedbackService(groqClient);

        var result = service.analyzeInterviewOverview(mcqScore, List.of());

        assertThat(result.readyLevel).isEqualTo(expectedReadyLevel);
    }
}
