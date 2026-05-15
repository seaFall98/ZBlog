package com.zblog.ops;

import com.zblog.common.api.ApiResponse;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system")
public class SystemAdminController {

  @GetMapping("/static")
  public ApiResponse<Map<String, Object>> staticInfo() {
    Runtime runtime = Runtime.getRuntime();
    return ApiResponse.ok(
        Map.ofEntries(
            Map.entry("cpu_core", runtime.availableProcessors()),
            Map.entry("cpu_model", System.getProperty("os.arch", "")),
            Map.entry("cpu_arch", System.getProperty("os.arch", "")),
            Map.entry("hostname", hostname()),
            Map.entry("os", System.getProperty("os.name", "")),
            Map.entry("server_ip", "127.0.0.1"),
            Map.entry("timezone", ZoneId.systemDefault().toString()),
            Map.entry("db_type", "PostgreSQL"),
            Map.entry("memory_total", runtime.maxMemory()),
            Map.entry("swap_total", 0L),
            Map.entry("disk_total", 0L),
            Map.entry("db_tables", 0),
            Map.entry("storage_status", "local"),
            Map.entry("email_status", "disabled"),
            Map.entry("feishu_status", "disabled"),
            Map.entry("app_version", "0.1.0")));
  }

  @GetMapping("/dynamic")
  public ApiResponse<Map<String, Object>> dynamicInfo() {
    Runtime runtime = Runtime.getRuntime();
    long used = runtime.totalMemory() - runtime.freeMemory();
    return ApiResponse.ok(
        Map.ofEntries(
            Map.entry("cpu_usage", 0),
            Map.entry("load_1", 0),
            Map.entry("load_5", 0),
            Map.entry("load_15", 0),
            Map.entry("memory_used", used),
            Map.entry("memory_available", runtime.maxMemory() - used),
            Map.entry("swap_used", 0),
            Map.entry("host_uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000),
            Map.entry("disk_used", 0),
            Map.entry("disk_free", 0),
            Map.entry("db_status", "UP"),
            Map.entry("db_size", 0),
            Map.entry("db_conn_count", 0),
            Map.entry("version_latest_version", "0.1.0"),
            Map.entry("version_last_check_error", "")));
  }

  @PostMapping("/check-update")
  public ApiResponse<Map<String, Object>> checkUpdate() {
    return ApiResponse.ok(
        Map.of(
            "has_update", false,
            "current_version", "0.1.0",
            "latest_version", "0.1.0",
            "versions", List.of(),
            "last_check_error", ""));
  }

  private String hostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception ignored) {
      return "localhost";
    }
  }
}
