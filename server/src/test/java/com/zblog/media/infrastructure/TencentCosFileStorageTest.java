package com.zblog.media.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.media.MediaStorageProperties;
import com.zblog.media.application.MediaStorageSettings.StorageSettings;
import org.junit.jupiter.api.Test;

class TencentCosFileStorageTest {

  @Test
  void deleteObjectKeyPrefersPersistedRemoteUrlPathOverCurrentPrefix() {
    TencentCosFileStorage storage = new TencentCosFileStorage(null, new MediaStorageProperties());
    StorageSettings settings =
        new StorageSettings("cos", "ap-guangzhou", "zblog-bucket", "https://cdn.example.com", "new-prefix", true);

    String key = storage.objectKey(settings, "remote.png", "https://cdn.example.com/old-prefix/remote%20file.png");

    assertThat(key).isEqualTo("old-prefix/remote file.png");
  }

  @Test
  void deleteObjectKeyStripsConfiguredDomainPathWhenPresent() {
    TencentCosFileStorage storage = new TencentCosFileStorage(null, new MediaStorageProperties());
    StorageSettings settings =
        new StorageSettings("cos", "ap-guangzhou", "zblog-bucket", "https://cdn.example.com/assets", "new-prefix", true);

    String key = storage.objectKey(settings, "remote.png", "https://cdn.example.com/assets/old-prefix/remote.png");

    assertThat(key).isEqualTo("old-prefix/remote.png");
  }

  @Test
  void deleteObjectKeyKeepsLiteralPlusInPersistedPath() {
    TencentCosFileStorage storage = new TencentCosFileStorage(null, new MediaStorageProperties());
    StorageSettings settings =
        new StorageSettings("cos", "ap-guangzhou", "zblog-bucket", "https://cdn.example.com", "new-prefix", true);

    String key = storage.objectKey(settings, "remote.png", "https://cdn.example.com/old-prefix/a+b%20c.png");

    assertThat(key).isEqualTo("old-prefix/a+b c.png");
  }

  @Test
  void deleteObjectKeyFallsBackToCurrentPrefixWhenPersistedUrlIsLocalOrBlank() {
    TencentCosFileStorage storage = new TencentCosFileStorage(null, new MediaStorageProperties());
    StorageSettings settings =
        new StorageSettings("cos", "ap-guangzhou", "zblog-bucket", "https://cdn.example.com", "new-prefix", true);

    assertThat(storage.objectKey(settings, "local.png", "/uploads/local.png")).isEqualTo("new-prefix/local.png");
    assertThat(storage.objectKey(settings, "blank.png", "")).isEqualTo("new-prefix/blank.png");
  }
}
