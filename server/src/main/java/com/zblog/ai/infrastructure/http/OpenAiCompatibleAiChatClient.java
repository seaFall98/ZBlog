package com.zblog.ai.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.ai.application.AiConfig;
import com.zblog.ai.application.port.AiChatClient;
import com.zblog.common.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleAiChatClient implements AiChatClient {

  // 适配 OpenAI-compatible Chat Completions，不绑定单一模型厂商。
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiCompatibleAiChatClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  public String completeChat(AiConfig config, String systemPrompt, String userContent) {
    try {
      URI endpoint = config.endpoint();
      Map<String, Object> payload =
          Map.of(
              "model", config.model(),
              "messages",
                  List.of(
                      Map.of("role", "system", "content", systemPrompt),
                      Map.of("role", "user", "content", userContent)),
              "temperature", 0.3,
              "stream", false);
      HttpRequest request =
          HttpRequest.newBuilder(endpoint)
              .timeout(Duration.ofSeconds(30))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + config.apiKey())
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
              .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException(40050, providerErrorMessage(response), HttpStatus.BAD_REQUEST);
      }
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode content = root.at("/choices/0/message/content");
      if (!content.isTextual() || content.asText().isBlank()) {
        throw new BusinessException(40051, "AI provider response is invalid", HttpStatus.BAD_REQUEST);
      }
      return content.asText().trim();
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(40052, "AI request failed", HttpStatus.BAD_REQUEST);
    }
  }

  private String providerErrorMessage(HttpResponse<String> response) {
    String detail = extractProviderMessage(response.body());
    if (!detail.isBlank()) {
      return "AI provider returned status " + response.statusCode() + ": " + detail;
    }
    return "AI provider returned status " + response.statusCode();
  }

  private String extractProviderMessage(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode message = root.at("/error/message");
      if (message.isTextual() && !message.asText().isBlank()) {
        return message.asText().trim();
      }
      JsonNode directMessage = root.get("message");
      if (directMessage != null && directMessage.isTextual() && !directMessage.asText().isBlank()) {
        return directMessage.asText().trim();
      }
    } catch (Exception ignored) {
      return body.length() > 300 ? body.substring(0, 300) : body;
    }
    return "";
  }
}
