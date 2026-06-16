package com.licenta.backend.code.controller;

import com.licenta.backend.code.dto.CodeRunRequestDto;
import com.licenta.backend.code.dto.CodeRunResponseDto;
import com.licenta.backend.code.dto.TestCaseDto;
import com.licenta.backend.code.service.CodeExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    public CodeExecutionController(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @GetMapping("/questions/{questionId}/testcases")
    public List<TestCaseDto> getTestCases(@PathVariable Long questionId) {
        return codeExecutionService.getTestCases(questionId);
    }

    @PostMapping("/code/run")
    public CodeRunResponseDto runCode(@RequestBody CodeRunRequestDto req) {
        return codeExecutionService.runCode(req);
    }
}
