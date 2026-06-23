package com.zblog.media.infrastructure;

import com.zblog.common.exception.BusinessException;
import com.zblog.media.MediaStorageProperties;
import com.zblog.media.application.MediaStorageSettings;
import com.zblog.media.application.MediaStorageSettings.StorageSettings;
import com.zblog.media.application.port.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TencentCosFileStorage implements FileStorage {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final MediaStorageSettings storageSettings;
  private final MediaStorageProperties properties;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public TencentCosFileStorage(MediaStorageSettings storageSettings, MediaStorageProperties properties) {
    this.storageSettings = storageSettings;
    this.properties = properties;
  }

  public String store(String filename, InputStream content) throws IOException {
    byte[] bytes = content.readAllBytes();
    StorageSettings settings = storageSettings.current();
    String key = objectKey(settings, filename);
    URI uri = objectUri(settings, key);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", authorization(settings, "put", key))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
            .build();
    send(request, "upload file to Tencent COS");
    return publicUrl(settings, key);
  }

  public void delete(String filename) throws IOException {
    StorageSettings settings = storageSettings.current();
    String key = objectKey(settings, filename);
    HttpRequest request =
        HttpRequest.newBuilder(objectUri(settings, key))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", authorization(settings, "delete", key))
            .DELETE()
            .build();
    send(request, "delete file from Tencent COS");
  }

  private void send(HttpRequest request, String operation) throws IOException {
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException(
            50201,
            "Failed to " + operation + ": Tencent COS returned HTTP " + response.statusCode(),
            HttpStatus.BAD_GATEWAY);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Tencent COS request interrupted", exception);
    }
  }

  private URI objectUri(StorageSettings settings, String key) {
    return URI.create("https://" + cosHost(settings) + "/" + encodePath(key));
  }

  private String publicUrl(StorageSettings settings, String key) {
    if (!settings.domain().isBlank()) {
      return settings.domain() + "/" + encodePath(key);
    }
    return "https://" + cosHost(settings) + "/" + encodePath(key);
  }

  private String cosHost(StorageSettings settings) {
    return settings.bucket() + ".cos." + settings.region() + ".myqcloud.com";
  }

  private String objectKey(StorageSettings settings, String filename) {
    String safeFilename = filename.replace('\\', '/').replaceAll("^/+", "");
    if (settings.prefix().isBlank()) {
      return safeFilename;
    }
    return settings.prefix() + "/" + safeFilename;
  }

  private String authorization(StorageSettings settings, String method, String key) {
    Instant now = Instant.now();
    long start = now.getEpochSecond() - 60;
    long end = now.plus(Duration.ofMinutes(10)).getEpochSecond();
    String keyTime = start + ";" + end;
    String httpString = method + "\n/" + encodePath(key) + "\n\nhost=" + cosHost(settings) + "\n";
    String stringToSign = "sha1\n" + keyTime + "\n" + sha1Hex(httpString) + "\n";
    String signKey = hmacSha1Hex(properties.getCosSecretKey(), keyTime);
    String signature = hmacSha1Hex(signKey, stringToSign);
    return "q-sign-algorithm=sha1&q-ak="
        + properties.getCosSecretId()
        + "&q-sign-time="
        + keyTime
        + "&q-key-time="
        + keyTime
        + "&q-header-list=host&q-url-param-list=&q-signature="
        + signature;
  }

  private String encodePath(String value) {
    String[] parts = value.split("/");
    StringBuilder encoded = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        encoded.append('/');
      }
      encoded.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20"));
    }
    return encoded.toString();
  }

  private String sha1Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).toLowerCase(Locale.ROOT);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String hmacSha1Hex(String key, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))).toLowerCase(Locale.ROOT);
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
