package com.zblog.media.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.common.api.PageResponse;
import com.zblog.media.application.port.FileRepository;
import com.zblog.media.application.port.FileStorage;
import com.zblog.media.application.port.FileStorageReference;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FileServiceDeleteTest {

  @Test
  void deleteUsesPersistedStorageReferenceAndMarksDatabaseAfterStorageDelete() {
    FakeFileRepository repository =
        new FakeFileRepository(List.of(new FileStorageReference("remote.png", "https://cdn.example.com/remote.png")));
    RecordingFileStorage storage = new RecordingFileStorage(repository);

    new FileService(repository, storage).delete(10L);

    assertThat(storage.deletedFilename).isEqualTo("remote.png");
    assertThat(storage.deletedFileUrl).isEqualTo("https://cdn.example.com/remote.png");
    assertThat(repository.markedDeleted).isTrue();
    assertThat(repository.markedAfterStorageDelete).isTrue();
  }

  private static final class RecordingFileStorage implements FileStorage {

    private final FakeFileRepository repository;
    private String deletedFilename;
    private String deletedFileUrl;

    private RecordingFileStorage(FakeFileRepository repository) {
      this.repository = repository;
    }

    @Override
    public String store(String filename, InputStream content) {
      return "";
    }

    @Override
    public void delete(String filename) throws IOException {
      throw new AssertionError("delete should include the persisted file URL");
    }

    @Override
    public void delete(String filename, String fileUrl) {
      deletedFilename = filename;
      deletedFileUrl = fileUrl;
      repository.storageDeleted = true;
    }
  }

  private static final class FakeFileRepository implements FileRepository {

    private final List<FileStorageReference> references;
    private boolean storageDeleted;
    private boolean markedDeleted;
    private boolean markedAfterStorageDelete;

    private FakeFileRepository(List<FileStorageReference> references) {
      this.references = references;
    }

    @Override
    public long create(
        String filename,
        String originalName,
        String fileUrl,
        String fileType,
        long fileSize,
        String uploadType) {
      return 0;
    }

    @Override
    public PageResponse<Map<String, Object>> list(
        int page,
        int pageSize,
        String keyword,
        String fileType,
        Integer status,
        String uploadType,
        Long minSize,
        Long maxSize,
        LocalDateTime start,
        LocalDateTime end) {
      return new PageResponse<>(List.of(), 0, page, pageSize);
    }

    @Override
    public List<FileStorageReference> findActiveStorageReferences(long id) {
      return references;
    }

    @Override
    public void markDeleted(long id) {
      markedDeleted = true;
      markedAfterStorageDelete = storageDeleted;
    }
  }
}
