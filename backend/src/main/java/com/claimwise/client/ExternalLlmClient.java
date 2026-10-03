package com.claimwise.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ExternalLlmClient {

    private final String apiKey;
    private final String provider;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ExternalLlmClient(
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.provider:openai}") String provider,
            @Value("${ai.model:gpt-4o-mini}") String model,
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.timeout-seconds:10}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.provider = provider != null ? provider.trim() : "openai";
        this.model = model != null ? model.trim() : "gpt-4o-mini";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://api.openai.com/v1";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .build();
    }

    public boolean isApiKeyConfigured() {
        return !apiKey.isEmpty();
    }

    public String getProviderInfo() {
        return provider + "/" + model;
    }

    public String generateReviewAnalysis(String systemPrompt, String userPrompt) throws Exception {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("AI API key is not configured");
        }

        String endpointUrl = baseUrl;
        if (!endpointUrl.endsWith("/chat/completions") && !endpointUrl.endsWith("/completions")) {
            if (endpointUrl.endsWith("/")) {
                endpointUrl += "chat/completions";
            } else {
                endpointUrl += "/chat/completions";
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        
        // Request JSON object format if supported by provider
        requestBody.put("response_format", Map.of("type", "json_object"));

        String payload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        log.info("Sending request to external LLM provider [{}] model [{}]...", provider, model);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("External LLM call failed with HTTP status code {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("LLM API returned status code " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText();
            if (content != null && !content.isBlank()) {
                return content;
            }
        }

        throw new RuntimeException("Malformed or empty response payload from external LLM provider");
    }
}
