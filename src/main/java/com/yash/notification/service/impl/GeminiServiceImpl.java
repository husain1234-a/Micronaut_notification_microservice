package com.yash.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.notification.config.GeminiConfig;
import com.yash.notification.service.GeminiService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.TaskExecutors;
import reactor.core.publisher.Mono;

@Singleton
public class GeminiServiceImpl implements GeminiService {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiServiceImpl.class);
    private final GeminiConfig geminiConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String GEMINI_URL = "/v1beta/models/gemini-1.5-flash-latest:generateContent?key=";

    @Inject
    public GeminiServiceImpl(GeminiConfig geminiConfig,
            @Client("https://generativelanguage.googleapis.com") HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.geminiConfig = geminiConfig;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> generateMessage(String prompt) {
        String url = GEMINI_URL + geminiConfig.getApiKey();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", new Object[] { part });
        Map<String, Object> body = new HashMap<>();
        body.put("contents", new Object[] { content });

        HttpRequest<Map<String, Object>> request = HttpRequest.POST(url, body)
                .contentType(MediaType.APPLICATION_JSON_TYPE);
        return Mono.from(httpClient.retrieve(request, String.class))
                .map(response -> {
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(response);
                        return jsonResponse
                                .path("candidates")
                                .path(0)
                                .path("content")
                                .path("parts")
                                .path(0)
                                .path("text")
                                .asText("AI is unable to generate message.");
                    } catch (Exception e) {
                        LOG.error("Error parsing Gemini API response: {}", e.getMessage(), e);
                        return "AI is unable to generate message. Error: " + e.getMessage();
                    }
                })
                .onErrorResume(e -> {
                    LOG.error("Error calling Gemini API: {}", e.getMessage(), e);
                    return Mono.just("AI is unable to generate message. Error: Unexpected error");
                });
    }
}