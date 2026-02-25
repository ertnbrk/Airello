package ai.planmate.ai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OpenAiProvider implements AiProvider {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.openai.timeout-seconds:30}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public boolean supportsTools() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AiResult invoke(String prompt, Map<String, Object> tools, Map<String, Object> context) {
        if (apiKey == null || apiKey.isBlank()) {
            return AiResult.failure("OpenAI API key not configured");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put(
                    "messages",
                    List.of(
                            Map.of(
                                    "role",
                                    "system",
                                    "content",
                                    "You are a project management AI assistant. "
                                            + "Respond with structured JSON when tools are "
                                            + "provided."),
                            Map.of("role", "user", "content", prompt)));
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.3);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + "/chat/completions"))
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error(
                        "OpenAI API error: status={ }, body={ }",
                        response.statusCode(),
                        response.body());
                return AiResult.failure("OpenAI API error: " + response.statusCode());
            }

            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) responseBody.get("choices");

            if (choices == null || choices.isEmpty()) {
                return AiResult.failure("No response from OpenAI");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            int totalTokens = usage != null ? (int) usage.getOrDefault("total_tokens", 0) : 0;

            Map<String, Object> toolOutput = new HashMap<>();
            try {
                Map<String, Object> parsed = objectMapper.readValue(content, Map.class);
                toolOutput.putAll(parsed);
            } catch (Exception e) {
                toolOutput.put("raw_content", content);
            }

            return AiResult.success(content, toolOutput, totalTokens);

        } catch (Exception e) {
            log.error("OpenAI provider error", e);
            return AiResult.failure("OpenAI provider error: " + e.getMessage());
        }
    }
}
