package com.licenta.backend.code.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.backend.code.dto.CodeRunRequestDto;
import com.licenta.backend.code.dto.CodeRunResponseDto;
import com.licenta.backend.code.dto.TestCaseDto;
import com.licenta.backend.questions.entity.TestCase;
import com.licenta.backend.questions.repo.TestCaseRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);
    private static final String JDOODLE_URL = "https://api.jdoodle.com/v1/execute";

    // JDoodle language id + versionIndex
    private static final Map<String, String[]> LANG_MAP = Map.ofEntries(
            Map.entry("java",       new String[]{"java",       "4"}),
            Map.entry("python",     new String[]{"python3",    "4"}),
            Map.entry("javascript", new String[]{"nodejs",     "4"}),
            Map.entry("typescript", new String[]{"typescript", "0"}),
            Map.entry("cpp",        new String[]{"cpp17",      "1"}),
            Map.entry("c++",        new String[]{"cpp17",      "1"}),
            Map.entry("csharp",     new String[]{"csharp",     "4"}),
            Map.entry("c#",         new String[]{"csharp",     "4"}),
            Map.entry("go",         new String[]{"go",         "4"}),
            Map.entry("kotlin",     new String[]{"kotlin",     "3"}),
            Map.entry("php",        new String[]{"php",        "4"}),
            Map.entry("ruby",       new String[]{"ruby",       "4"})
    );

    @Value("${jdoodle.client-id}")
    private String clientId;

    @Value("${jdoodle.client-secret}")
    private String clientSecret;

    private final TestCaseRepo testCaseRepo;
    private final JavaLocalExecutionService javaLocalExecutionService;
    private final PythonLocalExecutionService pythonLocalExecutionService;
    private final CLocalExecutionService cLocalExecutionService;
    private final CppLocalExecutionService cppLocalExecutionService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeExecutionService(TestCaseRepo testCaseRepo,
                                JavaLocalExecutionService javaLocalExecutionService,
                                PythonLocalExecutionService pythonLocalExecutionService,
                                CLocalExecutionService cLocalExecutionService,
                                CppLocalExecutionService cppLocalExecutionService) {
        this.testCaseRepo = testCaseRepo;
        this.javaLocalExecutionService = javaLocalExecutionService;
        this.pythonLocalExecutionService = pythonLocalExecutionService;
        this.cLocalExecutionService = cLocalExecutionService;
        this.cppLocalExecutionService = cppLocalExecutionService;
    }

    public List<TestCaseDto> getTestCases(Long questionId) {
        return testCaseRepo.findByQuestionIdOrderByOrderIndexAsc(questionId)
                .stream()
                .map(tc -> {
                    TestCaseDto dto = new TestCaseDto();
                    dto.id = tc.getId();
                    dto.description = tc.getDescription();
                    dto.inputData = tc.getInputData();
                    dto.expectedOutput = tc.getExpectedOutput();
                    dto.orderIndex = tc.getOrderIndex();
                    return dto;
                }).toList();
    }

    public CodeRunResponseDto runCode(CodeRunRequestDto req) {
        String language = req.language == null ? "" : req.language.toLowerCase();
        boolean useLocalRunner = isLocalRunnerSupported(language);
        String[] langInfo = LANG_MAP.getOrDefault(language, new String[]{language, "0"});

        List<TestCase> testCases = testCaseRepo.findByQuestionIdOrderByOrderIndexAsc(req.questionId);
        List<CodeRunResponseDto.TestCaseResultDto> results = new ArrayList<>();

        for (TestCase tc : testCases) {
            CodeRunResponseDto.TestCaseResultDto result = new CodeRunResponseDto.TestCaseResultDto();
            result.testCaseId = tc.getId();
            result.description = tc.getDescription();
            result.inputData = tc.getInputData();
            result.expectedOutput = tc.getExpectedOutput();

            long start = System.currentTimeMillis();
            try {
                if (useLocalRunner) {
                    JavaLocalExecutionService.ExecutionResult localResult =
                            runLocally(language, req.code, tc.getInputData());

                    result.executionTimeMs = localResult.executionTimeMs;
                    if (!localResult.success) {
                        result.error = localResult.error;
                        result.actualOutput = "";
                        result.passed = false;
                    } else {
                        String output = normalizeOutput(localResult.output);
                        result.actualOutput = output;
                        result.error = null;
                        result.passed = output.equals(normalizeOutput(tc.getExpectedOutput()));
                    }
                } else {
                    JDoodleResult jr = callJDoodle(langInfo[0], langInfo[1], req.code, tc.getInputData());
                    result.executionTimeMs = System.currentTimeMillis() - start;

                    String output = normalizeOutput(jr.output);

                    // JDoodle puts compile/runtime errors in the output field with specific patterns
                    if (jr.statusCode != 200 || output.startsWith("JDoodle") || output.contains("error:") || output.contains("Error:")) {
                        result.error = output;
                        result.actualOutput = "";
                        result.passed = false;
                    } else {
                        result.actualOutput = output;
                        result.error = null;
                        result.passed = output.equals(normalizeOutput(tc.getExpectedOutput()));
                    }
                }
            } catch (Exception e) {
                result.executionTimeMs = System.currentTimeMillis() - start;
                result.error = "Eroare la execuție: " + e.getMessage();
                result.actualOutput = "";
                result.passed = false;
                log.error("Code execution failed for testCase={}: {}", tc.getId(), e.getMessage());
            }

            results.add(result);
        }

        CodeRunResponseDto response = new CodeRunResponseDto();
        response.results = results;
        response.total = results.size();
        response.passed = (int) results.stream().filter(r -> r.passed).count();
        return response;
    }

    private boolean isJava(String language) {
        return "java".equals(language);
    }

    private boolean isPython(String language) {
        return "python".equals(language) || "python3".equals(language);
    }

    private boolean isC(String language) {
        return "c".equals(language);
    }

    private boolean isCpp(String language) {
        return "cpp".equals(language) || "c++".equals(language);
    }

    private boolean isLocalRunnerSupported(String language) {
        return isJava(language) || isPython(language) || isC(language) || isCpp(language);
    }

    private JavaLocalExecutionService.ExecutionResult runLocally(String language, String code, String inputData) {
        if (isJava(language)) {
            return javaLocalExecutionService.run(code, inputData);
        }
        if (isPython(language)) {
            return pythonLocalExecutionService.run(code, inputData);
        }
        if (isC(language)) {
            return cLocalExecutionService.run(code, inputData);
        }
        if (isCpp(language)) {
            return cppLocalExecutionService.run(code, inputData);
        }
        return JavaLocalExecutionService.ExecutionResult.error("Limbajul nu are runner local configurat.", 0);
    }

    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        return output.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

        private static class JDoodleResult {
        String output;
        int statusCode;
        String memory;
        String cpuTime;
    }
    
    private JDoodleResult callJDoodle(String language, String versionIndex, String code, String stdin) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "clientId", clientId,
                "clientSecret", clientSecret,
                "script", code,
                "stdin", stdin != null ? stdin : "",
                "language", language,
                "versionIndex", versionIndex
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(JDOODLE_URL, HttpMethod.POST, entity, String.class);

        JsonNode root = objectMapper.readTree(resp.getBody());
        JDoodleResult result = new JDoodleResult();
        result.output = root.path("output").asText("");
        result.statusCode = root.path("statusCode").asInt(200);
        result.memory = root.path("memory").asText("");
        result.cpuTime = root.path("cpuTime").asText("");
        return result;
    }


}
