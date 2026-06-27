package com.zblog.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zblog.cache.BlogCache;
import com.zblog.comment.application.port.CommentRepository;
import com.zblog.common.api.PageResponse;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.media.application.port.FileRepository;
import com.zblog.media.application.port.FileStorage;
import com.zblog.media.application.port.FileStorageReference;
import com.zblog.site.application.port.SettingRepository;
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

    new FileService(
            repository,
            storage,
            mock(UserRepository.class),
            mock(BlogCache.class),
            mock(CommentRepository.class),
            mock(SettingRepository.class))
        .delete(10L);

    assertThat(storage.deletedReference.filename()).isEqualTo("remote.png");
    assertThat(storage.deletedReference.fileUrl()).isEqualTo("https://cdn.example.com/remote.png");
    assertThat(repository.markedDeleted).isTrue();
    assertThat(repository.markedAfterStorageDelete).isTrue();
  }

  @Test
  void deletePassesPersistedStorageMetadataToStorage() {
    FakeFileRepository repository =
        new FakeFileRepository(
            List.of(
                new FileStorageReference(
                    "remote.png",
                    "https://new-cdn.example.com/remote.png",
                    "cos",
                    "old-bucket",
                    "ap-shanghai",
                    "old-prefix/remote.png",
                    "https://old-cdn.example.com",
                    "old-prefix")));
    RecordingFileStorage storage = new RecordingFileStorage(repository);

    new FileService(
            repository,
            storage,
            mock(UserRepository.class),
            mock(BlogCache.class),
            mock(CommentRepository.class),
            mock(SettingRepository.class))
        .delete(10L);

    assertThat(storage.deletedReference.storageProvider()).isEqualTo("cos");
    assertThat(storage.deletedReference.storageBucket()).isEqualTo("old-bucket");
    assertThat(storage.deletedReference.storageRegion()).isEqualTo("ap-shanghai");
    assertThat(storage.deletedReference.storageObjectKey()).isEqualTo("old-prefix/remote.png");
    assertThat(repository.markedAfterStorageDelete).isTrue();
  }

  @Test
  void deleteClearsSettingsThatPointAtDeletedFileUrl() {
    FakeFileRepository repository =
        new FakeFileRepository(List.of(new FileStorageReference("wall.png", "https://cdn.example.com/wall.png")));
    RecordingFileStorage storage = new RecordingFileStorage(repository);
    FakeSettingRepository settings = new FakeSettingRepository();

    new FileService(
            repository,
            storage,
            mock(UserRepository.class),
            mock(BlogCache.class),
            mock(CommentRepository.class),
            settings)
        .delete(10L);

    assertThat(settings.clearedValue).isEqualTo("https://cdn.example.com/wall.png");
  }

  private static final class RecordingFileStorage implements FileStorage {

    private final FakeFileRepository repository;
    private FileStorageReference deletedReference;

    private RecordingFileStorage(FakeFileRepository repository) {
      this.repository = repository;
    }

    @Override
    public String store(String filename, InputStream content) {
      return "";
    }

    @Override
    public void delete(String filename) throws IOException {
      throw new AssertionError("delete should include the persisted storage reference");
    }

    @Override
    public void delete(String filename, String fileUrl) {
      throw new AssertionError("delete should include the persisted storage reference");
    }

    @Override
    public void delete(FileStorageReference reference) {
      deletedReference = reference;
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

  private static final class FakeSettingRepository implements SettingRepository {

    private String clearedValue;

    @Override
    public Map<String, String> getGroup(String group) {
      return Map.of();
    }

    @Override
    public void upsert(String group, String key, String value) {}

    @Override
    public void deleteKey(String group, String key) {}

    @Override
    public int clearValuesEqualTo(String value) {
      clearedValue = value;
      return 1;
    }

    @Override
    public void deleteGroup(String group) {}
  }
}
