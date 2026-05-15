package com.zblog.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.site.application.SettingService;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiAdminService {

  private final SettingService settingService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final boolean allowPrivateNetwork;

  public AiAdminService(
      SettingService settingService,
      ObjectMapper objectMapper,
      @Value("${zblog.ai.allow-private-network:false}") boolean allowPrivateNetwork) {
    this.settingService = settingService;
    this.objectMapper = objectMapper;
    this.allowPrivateNetwork = allowPrivateNetwork;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  public Map<String, String> test(Map<String, String> request) {
    AiConfig config = configFrom(request);
    completeChat(config, "请回复 OK", "OK");
    return Map.of("status", "ok");
  }

  public Map<String, String> summary(Map<String, String> request) {
    String content = required(request.get("content"), "content");
    String prompt = setting("summary_prompt", "请用创作者视角为下面文章生成 50 到 100 字摘要。");
    String result = completeChat(configFrom(settingService.getGroup("ai")), prompt, content);
    return Map.of("summary", result);
  }

  public Map<String, String> aiSummary(Map<String, String> request) {
    String content = required(request.get("content"), "content");
    String prompt = setting("ai_summary_prompt", "请用旁观者视角为下面文章生成 150 到 200 字 AI 总结。");
    String result = completeChat(configFrom(settingService.getGroup("ai")), prompt, content);
    return Map.of("summary", result);
  }

  public Map<String, String> title(Map<String, String> request) {
    String content = required(request.get("content"), "content");
    String prompt = setting("title_prompt", "请为下面文章生成一个简洁标题，只返回标题文本。");
    String result = completeChat(configFrom(settingService.getGroup("ai")), prompt, content);
    return Map.of("title", result.replaceAll("^[《\"']|[》\"']$", ""));
  }

  private String completeChat(AiConfig config, String systemPrompt, String userContent) {
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
        throw new BusinessException(40050, "AI provider returned status " + response.statusCode(), HttpStatus.BAD_REQUEST);
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

  private AiConfig configFrom(Map<String, String> values) {
    return new AiConfig(
        validatedBaseUrl(required(values.get("base_url"), "base_url")),
        required(values.get("api_key"), "api_key"),
        required(values.get("model"), "model"));
  }

  private String validatedBaseUrl(String value) {
    try {
      URI uri = URI.create(value).normalize();
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new BusinessException(40054, "Only http and https AI base URLs are supported", HttpStatus.BAD_REQUEST);
      }
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        throw new BusinessException(40055, "Invalid AI base URL host", HttpStatus.BAD_REQUEST);
      }
      String asciiHost = IDN.toASCII(host);
      if (!allowPrivateNetwork && isPrivateHost(asciiHost)) {
        throw new BusinessException(40056, "Private network AI base URLs are not allowed", HttpStatus.BAD_REQUEST);
      }
      return new URI(uri.getScheme(), uri.getUserInfo(), asciiHost, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(40057, "Invalid AI base URL", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean isPrivateHost(String host) throws Exception {
    InetAddress address = InetAddress.getByName(host);
    return address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress();
  }

  private String setting(String key, String defaultValue) {
    String value = settingService.getGroup("ai").get(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(40053, name + " is required", HttpStatus.BAD_REQUEST);
    }
    return value.trim();
  }

  private record AiConfig(String baseUrl, String apiKey, String model) {
    URI endpoint() {
      String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
      if (!normalized.endsWith("/chat/completions")) {
        normalized += "/chat/completions";
      }
      return URI.create(normalized);
    }
  }
}
