package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.config.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiApiClient implements AiTextGenerator {

    private final AiFeatureProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public GeminiApiClient(AiFeatureProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createRestClient(properties));
    }

    GeminiApiClient(AiFeatureProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient createRestClient(AiFeatureProperties properties) {
        var requestFactory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .build());
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public Map<String, Object> generateJson(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.2,
                        "maxOutputTokens", 1200
                )
        );
        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/{model}:generateContent")
                            .build(properties.getModel()))
                    .header("x-goog-api-key", properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException exception) {
            throw new NotesAiService.AiUnavailableException(
                    "The AI service is temporarily unavailable. Please try again later.");
        }
        String json = responseText(response);
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (tools.jackson.core.JacksonException exception) {
            throw new NotesAiService.AiUnavailableException(
                    "The AI service returned an invalid response. Please try again.");
        }
    }

    private String responseText(Map<String, Object> response) {
        if (response == null) {
            throw new NotesAiService.AiUnavailableException(
                    "The AI service returned an incomplete response. Please try again.");
        }
        Object candidatesValue = response.get("candidates");
        if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()
                || !(candidates.get(0) instanceof Map<?, ?> candidate)
                || !(candidate.get("content") instanceof Map<?, ?> content)
                || !(content.get("parts") instanceof List<?> parts) || parts.isEmpty()
                || !(parts.get(0) instanceof Map<?, ?> part)
                || !(part.get("text") instanceof String text)) {
            throw new NotesAiService.AiUnavailableException(
                    "The AI service returned an incomplete response. Please try again.");
        }
        return text;
    }
}
