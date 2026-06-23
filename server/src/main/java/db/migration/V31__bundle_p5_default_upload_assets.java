package db.migration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V31__bundle_p5_default_upload_assets extends BaseJavaMigration {

  private static final List<String> DEFAULT_UPLOAD_FILENAMES =
      List.of(
          "1782184837997_0f0990627d6a8491243fb20c34c097c2.png",
          "1782184837996_ChatGPT_Image_May_19__2026__02_05_47_AM.png",
          "1782184837997_ChatGPT_Image_2026_6_12__06_00_01__4_.png");

  @Override
  public void migrate(Context context) throws Exception {
    Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();
    Files.createDirectories(uploadRoot);
    ClassLoader classLoader = getClass().getClassLoader();

    for (String filename : DEFAULT_UPLOAD_FILENAMES) {
      Path target = uploadRoot.resolve(filename).normalize();
      if (!target.startsWith(uploadRoot)) {
        throw new IllegalStateException("Default upload filename escapes upload root: " + filename);
      }
      if (Files.exists(target)) {
        continue;
      }

      String resourcePath = "db/default-uploads/" + filename;
      try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
        if (input == null) {
          throw new IllegalStateException("Missing bundled default upload asset: " + resourcePath);
        }
        Files.copy(input, target);
      }
    }
  }
}
