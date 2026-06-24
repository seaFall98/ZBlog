package com.zblog.media.application;

import com.zblog.media.application.MediaStorageSettings.StorageSettings;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MediaStorageStatusService {

  private final MediaStorageSettings storageSettings;

  public MediaStorageStatusService(MediaStorageSettings storageSettings) {
    this.storageSettings = storageSettings;
  }

  public Map<String, Object> status() {
    StorageSettings settings = storageSettings.current();
    return Map.of(
        "storage_type", settings.storageType(),
        "region", settings.region(),
        "bucket", settings.bucket(),
        "domain", settings.domain(),
        "prefix", settings.prefix(),
        "credential_configured", settings.credentialConfigured(),
        "ready", "local".equals(settings.storageType()) || settings.cosReady());
  }
}
