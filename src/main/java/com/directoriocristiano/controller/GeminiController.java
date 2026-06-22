package com.directoriocristiano.controller;

import com.directoriocristiano.dto.OptimizeRequest;
import com.directoriocristiano.dto.OptimizeResponse;
import com.directoriocristiano.gemini.IGeminiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final IGeminiService geminiService;

    @PostMapping("/optimize")
    public ResponseEntity<OptimizeResponse> optimize(@Valid @RequestBody OptimizeRequest request) {
        return ResponseEntity.ok(geminiService.optimize(request));
    }
}
