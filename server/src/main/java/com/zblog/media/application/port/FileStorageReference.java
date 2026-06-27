package com.zblog.media.application.port;

public record FileStorageReference(
    String filename,
    String fileUrl,
    String storageProvider,
    String storageBucket,
    String storageRegion,
    String storageObjectKey,
    String storageDomain,
    String storagePrefix) {

  public FileStorageReference(String filename, String fileUrl) {
    this(filename, fileUrl, "", "", "", "", "", "");
  }

  public FileStorageReference {
    filename = text(filename);
    fileUrl = text(fileUrl);
    storageProvider = text(storageProvider);
    storageBucket = text(storageBucket);
    storageRegion = text(storageRegion);
    storageObjectKey = text(storageObjectKey);
    storageDomain = trimTrailingSlashes(text(storageDomain));
    storagePrefix = trimSlashes(text(storagePrefix));
  }

  public boolean hasStorageMetadata() {
    return !storageProvider.isBlank() && !storageObjectKey.isBlank();
  }

  public boolean isStorageProvider(String provider) {
    return provider != null && provider.equalsIgnoreCase(storageProvider);
  }

  public FileStorageMetadata storageMetadata() {
    return new FileStorageMetadata(
        storageProvider, storageBucket, storageRegion, storageObjectKey, storageDomain, storagePrefix);
  }

  private static String text(String value) {
    return value == null ? "" : value.trim();
  }

  private static String trimTrailingSlashes(String value) {
    return value.replaceAll("/+$", "");
  }

  private static String trimSlashes(String value) {
    return value.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
  }
}
