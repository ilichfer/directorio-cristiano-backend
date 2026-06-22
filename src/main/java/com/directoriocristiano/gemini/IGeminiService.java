package com.directoriocristiano.gemini;

import com.directoriocristiano.dto.OptimizeRequest;
import com.directoriocristiano.dto.OptimizeResponse;

public interface IGeminiService {
    OptimizeResponse optimize(OptimizeRequest request);
}
