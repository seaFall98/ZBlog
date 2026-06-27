package com.zblog.media.infrastructure;

import com.zblog.common.exception.BusinessException;
import com.zblog.media.application.MediaStorageSettings;
import com.zblog.media.application.MediaStorageSettings.StorageSettings;
import com.zblog.media.application.port.FileStorage;
import com.zblog.media.application.port.FileStorageMetadata;
import com.zblog.media.application.port.FileStorageReference;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Primary
@Component
public class RoutingFileStorage implements FileStorage {

  private final FileStorage localFileStorage;
  private final TencentCosFileStorage tencentCosFileStorage;
  private final MediaStorageSettings storageSettings;

  public RoutingFileStorage(
      @Qualifier("localFileStorage") FileStorage localFileStorage,
      TencentCosFileStorage tencentCosFileStorage,
      MediaStorageSettings storageSettings) {
    this.localFileStorage = localFileStorage;
    this.tencentCosFileStorage = tencentCosFileStorage;
    this.storageSettings = storageSettings;
  }

  public String store(String filename, InputStream content) throws IOException {
    StorageSettings settings = storageSettings.current();
    if ("cos".equals(settings.storageType())) {
      if (!settings.cosReady()) {
        throw new BusinessException(40001, "Tencent COS storage is not fully configured", HttpStatus.BAD_REQUEST);
      }
      return tencentCosFileStorage.store(filename, content);
    }
    return localFileStorage.store(filename, content);
  }

  public void delete(String filename) throws IOException {
    delete(filename, "");
  }

  @Override
  public void delete(String filename, String fileUrl) throws IOException {
    StorageSettings settings = storageSettings.current();
    if (isRemoteUrl(fileUrl)) {
      if (!settings.cosConfigured()) {
        throw new BusinessException(40001, "Tencent COS storage is not fully configured", HttpStatus.BAD_REQUEST);
      }
      tencentCosFileStorage.delete(filename, fileUrl);
      return;
    }
    if (isLocalUrl(fileUrl)) {
      localFileStorage.delete(filename);
      return;
    }
    if ("cos".equals(settings.storageType()) && settings.cosReady()) {
      tencentCosFileStorage.delete(filename);
      return;
    }
    localFileStorage.delete(filename);
  }

  @Override
  public void delete(FileStorageReference reference) throws IOException {
    if (reference.hasStorageMetadata()) {
      if (reference.isStorageProvider("cos")) {
        tencentCosFileStorage.delete(reference);
        return;
      }
      if (reference.isStorageProvider("local")) {
        localFileStorage.delete(reference.storageObjectKey());
        return;
      }
    }
    delete(reference.filename(), reference.fileUrl());
  }

  @Override
  public FileStorageMetadata metadata(String filename, String fileUrl) {
    StorageSettings settings = storageSettings.current();
    if (isRemoteUrl(fileUrl) || ("cos".equals(settings.storageType()) && settings.cosReady())) {
      return tencentCosFileStorage.metadata(filename, fileUrl);
    }
    return localFileStorage.metadata(filename, fileUrl);
  }

  private boolean isRemoteUrl(String fileUrl) {
    return fileUrl != null && (fileUrl.startsWith("http://") || fileUrl.startsWith("https://"));
  }

  private boolean isLocalUrl(String fileUrl) {
    return fileUrl != null && fileUrl.startsWith("/uploads/");
  }
}
