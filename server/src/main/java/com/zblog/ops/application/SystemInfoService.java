package com.zblog.ops.application;

import com.zblog.ops.application.port.SystemDatabaseInfoRepository;
import com.zblog.ops.application.port.SystemRuntimeInfoProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SystemInfoService {

  private final SystemRuntimeInfoProvider runtimeInfoProvider;
  private final SystemDatabaseInfoRepository databaseInfoRepository;

  public SystemInfoService(
      SystemRuntimeInfoProvider runtimeInfoProvider,
      SystemDatabaseInfoRepository databaseInfoRepository) {
    this.runtimeInfoProvider = runtimeInfoProvider;
    this.databaseInfoRepository = databaseInfoRepository;
  }

  public Map<String, Object> staticInfo() {
    Map<String, Object> info = new LinkedHashMap<>(runtimeInfoProvider.staticInfo());
    info.put("db_type", databaseInfoRepository.productName());
    info.put("db_tables", databaseInfoRepository.tableCount());
    return info;
  }

  public Map<String, Object> dynamicInfo() {
    Map<String, Object> info = new LinkedHashMap<>(runtimeInfoProvider.dynamicInfo());
    info.put("db_status", databaseInfoRepository.status());
    return info;
  }

  public Map<String, Object> checkUpdate() {
    return Map.of(
        "has_update", false,
        "current_version", "0.1.0",
        "latest_version", "unsupported",
        "versions", List.of(),
        "update_check_status", "unsupported",
        "last_check_error", "No remote update source is configured");
  }
}
