package com.directoriocristiano.gemini;

import com.directoriocristiano.dto.OptimizeRequest;
import com.directoriocristiano.dto.OptimizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GeminiServiceImpl implements IGeminiService {

    private final RestClient restClient;
    private final String apiKey;

    public GeminiServiceImpl(
            @Value("${app.gemini.api-url}") String apiUrl,
            @Value("${app.gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create(apiUrl);
    }

    public OptimizeResponse optimize(OptimizeRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return getFallbackResponse(request);
        }

        String prompt = buildPrompt(request);

        GeminiRequest geminiRequest = new GeminiRequest(
                List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt))))
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri("?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            return parseResponse(response, request);
        } catch (Exception e) {
            return getFallbackResponse(request);
        }
    }

    private String buildPrompt(OptimizeRequest request) {
        String values = request.coreValues() != null && !request.coreValues().isEmpty()
                ? "Valores: " + String.join(", ", request.coreValues()) + ". "
                : "";

        String aspiration = request.bibleAspiration() != null && !request.bibleAspiration().isBlank()
                ? "Aspiraci\u00f3n b\u00edblica: " + request.bibleAspiration() + ". "
                : "";

        return """
                Eres un experto en marketing para negocios cristianos. Optimiza la siguiente descripci\u00f3n de negocio.
                
                Nombre del negocio: %s
                Categor\u00eda: %s
                %s
                %s
                Descripci\u00f3n actual: %s
                
                Devuelve \u00daNICAMENTE un objeto JSON v\u00e1lido con esta estructura exacta (sin markdown, sin ```, solo JSON):
                {
                  "optimizedDescription": "descripci\u00f3n mejorada con enfoque cristiano",
                  "recommendedSlogan": "eslogan corto y poderoso",
                  "generatedTags": ["tag1", "tag2", "tag3"],
                  "biblicalConnectionQuote": "verso b\u00edblico relacionado"
                }
                """.formatted(request.name(), request.category(), values, aspiration, request.rawDescription());
    }

    private OptimizeResponse parseResponse(GeminiResponse response, OptimizeRequest request) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return getFallbackResponse(request);
        }

        String text = response.candidates().getFirst().content().parts().getFirst().text();
        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(text, OptimizeResponse.class);
        } catch (Exception e) {
            return getFallbackResponse(request);
        }
    }

    private OptimizeResponse getFallbackResponse(OptimizeRequest request) {
        String description = request.rawDescription().length() > 100
                ? request.rawDescription().substring(0, 100) + "..."
                : request.rawDescription();

        return new OptimizeResponse(
                description + " \u2014 Servicio con excelencia y valores cristianos.",
                "Calidad y fe en " + request.name(),
                List.of(request.category(), "Cristiano", "Confiable"),
                "Proverbios 22:29 - \u00bfHas visto a un hombre h\u00e1bil en su trabajo? Delante de los reyes estar\u00e1."
        );
    }
}
