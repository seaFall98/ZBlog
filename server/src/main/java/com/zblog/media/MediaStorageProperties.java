package com.zblog.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zblog.media.storage")
public class MediaStorageProperties {

  private String cosSecretId = "";
  private String cosSecretKey = "";

  public String getCosSecretId() {
    return cosSecretId;
  }

  public void setCosSecretId(String cosSecretId) {
    this.cosSecretId = cosSecretId;
  }

  public String getCosSecretKey() {
    return cosSecretKey;
  }

  public void setCosSecretKey(String cosSecretKey) {
    this.cosSecretKey = cosSecretKey;
  }

  public boolean hasCosCredentials() {
    return !cosSecretId.isBlank() && !cosSecretKey.isBlank();
  }
}
