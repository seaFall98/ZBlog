package com.zblog.media.application.port;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {

  String store(String filename, InputStream content) throws IOException;

  void delete(String filename) throws IOException;

  default void delete(String filename, String fileUrl) throws IOException {
    delete(filename);
  }

  default void delete(FileStorageReference reference) throws IOException {
    delete(reference.filename(), reference.fileUrl());
  }

  default FileStorageMetadata metadata(String filename, String fileUrl) {
    return FileStorageMetadata.empty();
  }
}
