package com.sfgroup81.tams.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class OpenAiCompatibleChatClient implements AiChatClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public OpenAiCompatibleChatClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    }

    OpenAiCompatibleChatClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", AiModelConfig.MODEL,
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object"),
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    }
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AiModelConfig.BASE_URL + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + AiModelConfig.API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI screening request failed with status " + response.statusCode());
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("AI screening returned no content");
            }
            return content.asText();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to call AI screening model", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call AI screening model", ex);
        }
    }
}
