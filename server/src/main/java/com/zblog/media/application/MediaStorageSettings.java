package com.zblog.media.application;

import com.zblog.media.MediaStorageProperties;
import com.zblog.site.application.port.SettingRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MediaStorageSettings {

  private final SettingRepository settingRepository;
  private final MediaStorageProperties properties;

  public MediaStorageSettings(SettingRepository settingRepository, MediaStorageProperties properties) {
    this.settingRepository = settingRepository;
    this.properties = properties;
  }

  public StorageSettings current() {
    Map<String, String> values = settingRepository.getGroup("upload");
    String type = read(values, "storage_type", "local").toLowerCase();
    if (!"cos".equals(type)) {
      type = "local";
    }
    return new StorageSettings(
        type,
        read(values, "region", ""),
        read(values, "bucket", ""),
        normalizeDomain(read(values, "domain", read(values, "endpoint", ""))),
        normalizePrefix(read(values, "prefix", read(values, "path_prefix", ""))),
        properties.hasCosCredentials());
  }

  private String read(Map<String, String> values, String key, String fallback) {
    String direct = values.get(key);
    if (direct != null) {
      return direct;
    }
    return values.getOrDefault("upload." + key, fallback);
  }

  private String normalizeDomain(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().replaceAll("/+$", "");
  }

  private String normalizePrefix(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String prefix = value.trim().replace('\\', '/');
    prefix = prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    return prefix;
  }

  public record StorageSettings(
      String storageType,
      String region,
      String bucket,
      String domain,
      String prefix,
      boolean credentialConfigured) {

    public boolean cosReady() {
      return "cos".equals(storageType) && !region.isBlank() && !bucket.isBlank() && credentialConfigured;
    }
  }
}
