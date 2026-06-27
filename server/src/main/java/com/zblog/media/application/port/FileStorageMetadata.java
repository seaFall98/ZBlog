package com.zblog.media.application.port;

public record FileStorageMetadata(
    String provider,
    String bucket,
    String region,
    String objectKey,
    String domain,
    String prefix) {

  public static FileStorageMetadata empty() {
    return new FileStorageMetadata("", "", "", "", "", "");
  }
}
