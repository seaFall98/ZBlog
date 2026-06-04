package com.zblog.ai.application;

import java.net.URI;

public record AiConfig(String baseUrl, String apiKey, String model) {

  public URI endpoint() {
    String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    if (!normalized.endsWith("/chat/completions")) {
      normalized += "/chat/completions";
    }
    return URI.create(normalized);
  }
}
