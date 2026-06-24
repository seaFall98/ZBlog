package com.zblog.media.infrastructure;

import com.zblog.media.application.port.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component("localFileStorage")
public class LocalFileStorage implements FileStorage {

  private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

  public String store(String filename, InputStream content) throws IOException {
    Files.createDirectories(uploadRoot);
    Path target = uploadRoot.resolve(filename).normalize();
    Files.copy(content, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    return "/uploads/" + filename;
  }

  public void delete(String filename) throws IOException {
    Path target = uploadRoot.resolve(filename).normalize();
    if (target.startsWith(uploadRoot)) {
      Files.deleteIfExists(target);
    }
  }
}
