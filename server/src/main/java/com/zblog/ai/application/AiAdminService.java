package com.zblog.ai.application;

import com.zblog.ai.application.port.AiChatClient;
import com.zblog.common.exception.BusinessException;
import com.zblog.site.application.SettingService;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiAdminService {

  // AI 管理用例同时处理站点配置读取和远程模型端点安全边界。
  private final SettingService settingService;
  private final AiChatClient aiChatClient;
  private final boolean allowPrivateNetwork;

  public AiAdminService(
      SettingService settingService,
      AiChatClient aiChatClient,
      @Value("${zblog.ai.allow-private-network:false}") boolean allowPrivateNetwork) {
    this.settingService = settingService;
    this.aiChatClient = aiChatClient;
    this.allowPrivateNetwork = allowPrivateNetwork;
  }

  public Map<String, Object> test(Map<String, Object> request) {
    AiConfig config = configFrom(request);
    aiChatClient.completeChat(config, "请回复 OK", "OK");
    return Map.of("status", "ok");
  }

  public Map<String, Object> summary(Map<String, Object> request) {
    String content = required(text(request.get("content")), "content");
    String prompt = setting("summary_prompt", "请用创作者视角为下面文章生成 50 到 100 字摘要。");
    return generateSummary(content, prompt, 100);
  }

  public Map<String, Object> aiSummary(Map<String, Object> request) {
    String content = required(text(request.get("content")), "content");
    String prompt = setting("ai_summary_prompt", "请用旁观者视角为下面文章生成 150 到 200 字 AI 总结。");
    return generateSummary(content, prompt, 200);
  }

  public Map<String, Object> title(Map<String, Object> request) {
    String content = required(text(request.get("content")), "content");
    String prompt = setting("title_prompt", "请为下面文章生成一个简洁标题，只返回标题文本。");
    String result = aiChatClient.completeChat(configFrom(settingService.getGroup("ai")), prompt, content);
    return Map.of("title", result.replaceAll("^[《\"']|[》\"']$", ""));
  }

  private Map<String, Object> generateSummary(String content, String prompt, int maxLength) {
    AiConfig config = configFrom(settingService.getGroup("ai"));
    String constrainedPrompt = summaryPrompt(prompt, maxLength);
    String result = aiChatClient.completeChat(config, constrainedPrompt, content).strip();
    int originalLength = result.length();
    boolean regenerated = false;
    if (originalLength > maxLength) {
      result = aiChatClient.completeChat(config, compressionPrompt(maxLength), result).strip();
      regenerated = true;
    }
    boolean overLimit = result.length() > maxLength;
    return Map.of(
        "summary", result,
        "trimmed", false,
        "over_limit", overLimit,
        "regenerated", regenerated,
        "max_length", maxLength,
        "original_length", originalLength,
        "final_length", result.length());
  }

  private String summaryPrompt(String prompt, int maxLength) {
    return prompt
        + "\n\n长度要求：最终只输出一段完整摘要，不要列表，不要解释，不要超过"
        + maxLength
        + "个中文字符；必须在句子结束处收束，不能输出半句话或被截断的句子。";
  }

  private String compressionPrompt(int maxLength) {
    return "请把下面摘要改写为不超过"
        + maxLength
        + "个中文字符的一段完整摘要。只输出改写后的摘要，不要解释；必须语义完整，不能输出半句话。";
  }

  private AiConfig configFrom(Map<String, ?> values) {
    return new AiConfig(
        validatedBaseUrl(required(configValue(values, "base_url"), "base_url")),
        required(configValue(values, "api_key"), "api_key"),
        required(configValue(values, "model"), "model"));
  }

  private String configValue(Map<String, ?> values, String key) {
    String value = text(values.get("ai." + key));
    if (value.isBlank()) {
      value = text(values.get(key));
    }
    return value;
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
        // 默认拒绝私网、环回、链路本地和组播地址，避免 AI base URL 成为 SSRF 入口。
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
    Map<String, String> values = settingService.getGroup("ai");
    String value = values.get("ai." + key);
    if (value == null || value.isBlank()) {
      value = values.get(key);
    }
    // prompt 支持站点配置覆盖，默认中文 prompt 只是未配置时的安全 fallback。
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(40053, name + " is required", HttpStatus.BAD_REQUEST);
    }
    return value.trim();
  }

  private String text(Object value) {
    return value == null ? "" : value.toString();
  }
}
