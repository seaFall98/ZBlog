package com.zblog.media.infrastructure;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.zblog.common.exception.BusinessException;
import com.zblog.media.MediaStorageProperties;
import com.zblog.media.application.MediaStorageSettings;
import com.zblog.media.application.MediaStorageSettings.StorageSettings;
import com.zblog.media.application.port.FileStorage;
import com.zblog.media.application.port.FileStorageMetadata;
import com.zblog.media.application.port.FileStorageReference;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

@Component
public class TencentCosFileStorage implements FileStorage {

  private final MediaStorageSettings storageSettings;
  private final MediaStorageProperties properties;
  private final ConcurrentMap<String, COSClient> clients = new ConcurrentHashMap<>();

  public TencentCosFileStorage(MediaStorageSettings storageSettings, MediaStorageProperties properties) {
    this.storageSettings = storageSettings;
    this.properties = properties;
  }

  public String store(String filename, InputStream content) throws IOException {
    byte[] bytes = content.readAllBytes();
    StorageSettings settings = storageSettings.current();
    String key = objectKey(settings, filename);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(bytes.length);
    COSClient client = clientFor(settings);

    try {
      client.putObject(new PutObjectRequest(settings.bucket(), key, new java.io.ByteArrayInputStream(bytes), metadata));
      return publicUrl(settings, key);
    } catch (CosClientException exception) {
      throw new BusinessException(
          50201,
          "Failed to upload file to Tencent COS: " + exception.getMessage(),
          HttpStatus.BAD_GATEWAY);
    }
  }

  public void delete(String filename) throws IOException {
    delete(filename, "");
  }

  @Override
  public void delete(String filename, String fileUrl) throws IOException {
    StorageSettings settings = storageSettings.current();
    String key = objectKey(settings, filename, fileUrl);
    COSClient client = clientFor(settings);
    try {
      client.deleteObject(settings.bucket(), key);
    } catch (CosClientException exception) {
      throw new BusinessException(
          50201,
          "Failed to delete file from Tencent COS: " + exception.getMessage(),
          HttpStatus.BAD_GATEWAY);
    }
  }

  @Override
  public void delete(FileStorageReference reference) throws IOException {
    if (reference.hasStorageMetadata() && reference.isStorageProvider("cos")) {
      StorageSettings settings = settingsFromReference(reference);
      if (!settings.cosConfigured()) {
        throw new BusinessException(40001, "Tencent COS storage is not fully configured", HttpStatus.BAD_REQUEST);
      }
      COSClient client = clientFor(settings);
      try {
        client.deleteObject(settings.bucket(), reference.storageObjectKey());
      } catch (CosClientException exception) {
        throw new BusinessException(
            50201,
            "Failed to delete file from Tencent COS: " + exception.getMessage(),
            HttpStatus.BAD_GATEWAY);
      }
      return;
    }
    delete(reference.filename(), reference.fileUrl());
  }

  @Override
  public FileStorageMetadata metadata(String filename, String fileUrl) {
    StorageSettings settings = storageSettings.current();
    String key = objectKey(settings, filename, fileUrl);
    return new FileStorageMetadata(
        "cos", settings.bucket(), settings.region(), key, settings.domain(), settings.prefix());
  }

  private StorageSettings settingsFromReference(FileStorageReference reference) {
    FileStorageMetadata metadata = reference.storageMetadata();
    return new StorageSettings(
        "cos",
        metadata.region(),
        metadata.bucket(),
        metadata.domain(),
        metadata.prefix(),
        properties.hasCosCredentials());
  }

  String objectKey(StorageSettings settings, String filename, String fileUrl) {
    if (fileUrl != null && (fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))) {
      if (!settings.domain().isBlank() && fileUrl.startsWith(settings.domain() + "/")) {
        return decodePath(fileUrl.substring(settings.domain().length() + 1));
      }
      try {
        String path = URI.create(fileUrl).getRawPath();
        if (path != null && !path.isBlank()) {
          return decodePath(path.replaceAll("^/+", ""));
        }
      } catch (IllegalArgumentException ignored) {
        // Fall back to the configured prefix + filename for malformed historical URLs.
      }
    }
    return objectKey(settings, filename);
  }

  private String decodePath(String path) {
    return UriUtils.decode(path.replace('\\', '/').replaceAll("^/+", ""), StandardCharsets.UTF_8);
  }

  private COSClient clientFor(StorageSettings settings) {
    return clients.computeIfAbsent(clientKey(settings), ignored -> createClient(settings));
  }

  private COSClient createClient(StorageSettings settings) {
    COSCredentials credentials = new BasicCOSCredentials(properties.getCosSecretId(), properties.getCosSecretKey());
    ClientConfig clientConfig = new ClientConfig(new Region(settings.region()));
    clientConfig.setHttpProtocol(HttpProtocol.https);
    return new COSClient(credentials, clientConfig);
  }

  private String clientKey(StorageSettings settings) {
    return settings.region() + "|" + properties.getCosSecretId();
  }

  @PreDestroy
  public void shutdownClients() {
    clients.values().forEach(COSClient::shutdown);
    clients.clear();
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
}
