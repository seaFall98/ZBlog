package com.zblog.site.application;

import com.zblog.site.application.port.SettingRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

  private final SettingRepository settingRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  public SettingService(SettingRepository settingRepository) {
    this.settingRepository = settingRepository;
  }

  public Map<String, String> getGroup(String group) {
    return settingRepository.getGroup(group);
  }

  @Transactional
  public Map<String, String> updateGroup(String group, Map<String, String> values) {
    values.forEach((key, value) -> settingRepository.upsert(group, key, value == null ? "" : value));
    return getGroup(group);
  }

  public Map<String, String> resetMcpSecret() {
    byte[] bytes = new byte[24];
    secureRandom.nextBytes(bytes);
    String secret = HexFormat.of().formatHex(bytes);
    settingRepository.upsert("ai", "mcp_secret", secret);
    return Map.of("secret", secret);
  }
}
