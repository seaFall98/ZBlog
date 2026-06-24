package com.zblog.media.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.media.MediaStorageProperties;
import com.zblog.media.application.MediaStorageSettings;
import com.zblog.media.application.port.FileStorage;
import com.zblog.site.application.port.SettingRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutingFileStorageTest {

  @Test
  void deleteRemoteFileUsesCosStorageEvenWhenCurrentStorageModeIsLocal() throws Exception {
    RecordingFileStorage localStorage = new RecordingFileStorage();
    MediaStorageSettings settings = mediaStorageSettings("local");
    RecordingTencentCosFileStorage cosStorage = new RecordingTencentCosFileStorage(settings);
    RoutingFileStorage routingFileStorage = new RoutingFileStorage(localStorage, cosStorage, settings);

    routingFileStorage.delete("remote.png", "https://cdn.example.com/remote.png");

    assertThat(cosStorage.deletedFilenames).containsExactly("remote.png");
    assertThat(cosStorage.deletedFileUrls).containsExactly("https://cdn.example.com/remote.png");
    assertThat(localStorage.deletedFilenames).isEmpty();
  }

  @Test
  void deleteWithoutPersistedUrlKeepsUsingCurrentStorageMode() throws Exception {
    RecordingFileStorage localStorage = new RecordingFileStorage();
    MediaStorageSettings settings = mediaStorageSettings("cos");
    RecordingTencentCosFileStorage cosStorage = new RecordingTencentCosFileStorage(settings);
    RoutingFileStorage routingFileStorage = new RoutingFileStorage(localStorage, cosStorage, settings);

    routingFileStorage.delete("legacy.png");

    assertThat(cosStorage.deletedFilenames).containsExactly("legacy.png");
    assertThat(localStorage.deletedFilenames).isEmpty();
  }

  private MediaStorageSettings mediaStorageSettings(String storageType) {
    MediaStorageProperties properties = new MediaStorageProperties();
    properties.setCosSecretId("secret-id");
    properties.setCosSecretKey("secret-key");
    return new MediaStorageSettings(new FakeSettingRepository(storageType), properties);
  }

  private static final class RecordingFileStorage implements FileStorage {

    private final java.util.ArrayList<String> deletedFilenames = new java.util.ArrayList<>();

    @Override
    public String store(String filename, InputStream content) {
      return "/uploads/" + filename;
    }

    @Override
    public void delete(String filename) {
      deletedFilenames.add(filename);
    }
  }

  private static final class RecordingTencentCosFileStorage extends TencentCosFileStorage {

    private final java.util.ArrayList<String> deletedFilenames = new java.util.ArrayList<>();
    private final java.util.ArrayList<String> deletedFileUrls = new java.util.ArrayList<>();

    private RecordingTencentCosFileStorage(MediaStorageSettings settings) {
      super(settings, new MediaStorageProperties());
    }

    @Override
    public void delete(String filename) {
      deletedFilenames.add(filename);
    }

    @Override
    public void delete(String filename, String fileUrl) {
      deletedFilenames.add(filename);
      deletedFileUrls.add(fileUrl);
    }
  }

  private static final class FakeSettingRepository implements SettingRepository {

    private final String storageType;

    private FakeSettingRepository(String storageType) {
      this.storageType = storageType;
    }

    @Override
    public Map<String, String> getGroup(String group) {
      return Map.of(
          "storage_type", storageType,
          "region", "ap-guangzhou",
          "bucket", "zblog-bucket",
          "domain", "https://cdn.example.com",
          "prefix", "media");
    }

    @Override
    public void upsert(String group, String key, String value) {}

    @Override
    public void deleteKey(String group, String key) {}

    @Override
    public void deleteGroup(String group) {}
  }
}
