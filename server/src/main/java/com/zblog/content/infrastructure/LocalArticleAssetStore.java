package com.zblog.content.infrastructure;

import com.zblog.content.application.port.ArticleAssetStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LocalArticleAssetStore implements ArticleAssetStore {

  private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

  public boolean existsUpload(String url) {
    return resolveUpload(url).filter(Files::exists).isPresent();
  }

  public Optional<byte[]> readUpload(String url) {
    return resolveUpload(url)
        .filter(Files::exists)
        .map(
            path -> {
              try {
                return Files.readAllBytes(path);
              } catch (IOException exception) {
                throw new IllegalStateException("Failed to read article asset", exception);
              }
            });
  }

  private Optional<Path> resolveUpload(String url) {
    if (url == null || !url.startsWith("/uploads/")) {
      return Optional.empty();
    }
    String filename = url.substring("/uploads/".length());
    Path asset = uploadRoot.resolve(filename).normalize();
    return asset.startsWith(uploadRoot) ? Optional.of(asset) : Optional.empty();
  }
}
