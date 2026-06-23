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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TencentCosFileStorage implements FileStorage {

  private final MediaStorageSettings storageSettings;
  private final MediaStorageProperties properties;

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
    COSClient client = withClient(settings);

    try {
      client.putObject(new PutObjectRequest(settings.bucket(), key, new java.io.ByteArrayInputStream(bytes), metadata));
      return publicUrl(settings, key);
    } catch (CosClientException exception) {
      throw new BusinessException(
          50201,
          "Failed to upload file to Tencent COS: " + exception.getMessage(),
          HttpStatus.BAD_GATEWAY);
    } finally {
      client.shutdown();
    }
  }

  public void delete(String filename) throws IOException {
    StorageSettings settings = storageSettings.current();
    String key = objectKey(settings, filename);
    COSClient client = withClient(settings);
    try {
      client.deleteObject(settings.bucket(), key);
    } catch (CosClientException exception) {
      throw new BusinessException(
          50201,
          "Failed to delete file from Tencent COS: " + exception.getMessage(),
          HttpStatus.BAD_GATEWAY);
    } finally {
      client.shutdown();
    }
  }

  private COSClient withClient(StorageSettings settings) {
    COSCredentials credentials = new BasicCOSCredentials(properties.getCosSecretId(), properties.getCosSecretKey());
    ClientConfig clientConfig = new ClientConfig(new Region(settings.region()));
    clientConfig.setHttpProtocol(HttpProtocol.https);
    return new COSClient(credentials, clientConfig);
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
