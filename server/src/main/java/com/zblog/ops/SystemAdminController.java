package com.zblog.ops;

import com.zblog.common.api.ApiResponse;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system")
public class SystemAdminController {

  private final JdbcTemplate jdbcTemplate;

  public SystemAdminController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/static")
  public ApiResponse<Map<String, Object>> staticInfo() {
    Runtime runtime = Runtime.getRuntime();
    File root = new File(".").getAbsoluteFile();
    return ApiResponse.ok(
        Map.ofEntries(
            Map.entry("cpu_core", runtime.availableProcessors()),
            Map.entry("cpu_model", System.getProperty("os.arch", "unsupported")),
            Map.entry("cpu_arch", System.getProperty("os.arch", "unsupported")),
            Map.entry("hostname", hostname()),
            Map.entry("os", System.getProperty("os.name", "unsupported")),
            Map.entry("server_ip", serverIp()),
            Map.entry("timezone", ZoneId.systemDefault().toString()),
            Map.entry("db_type", databaseProductName()),
            Map.entry("memory_total", runtime.maxMemory()),
            Map.entry("swap_total", "unsupported"),
            Map.entry("disk_total", root.getTotalSpace()),
            Map.entry("db_tables", tableCount()),
            Map.entry("storage_status", "local"),
            Map.entry("email_status", "disabled"),
            Map.entry("feishu_status", "disabled"),
            Map.entry("app_version", "0.1.0")));
  }

  @GetMapping("/dynamic")
  public ApiResponse<Map<String, Object>> dynamicInfo() {
    Runtime runtime = Runtime.getRuntime();
    File root = new File(".").getAbsoluteFile();
    long used = runtime.totalMemory() - runtime.freeMemory();
    return ApiResponse.ok(
        Map.ofEntries(
            Map.entry("cpu_usage", "unsupported"),
            Map.entry("cpu_usage_status", "unsupported"),
            Map.entry("load_1", systemLoadAverage()),
            Map.entry("load_5", "unsupported"),
            Map.entry("load_15", "unsupported"),
            Map.entry("memory_used", used),
            Map.entry("memory_available", runtime.maxMemory() - used),
            Map.entry("swap_used", "unsupported"),
            Map.entry("host_uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000),
            Map.entry("disk_used", root.getTotalSpace() - root.getFreeSpace()),
            Map.entry("disk_free", root.getFreeSpace()),
            Map.entry("db_status", dbStatus()),
            Map.entry("db_size", "unsupported"),
            Map.entry("db_conn_count", "unsupported"),
            Map.entry("version_latest_version", "unsupported"),
            Map.entry("version_last_check_error", "update check unsupported in local backend")));
  }

  @PostMapping("/check-update")
  public ApiResponse<Map<String, Object>> checkUpdate() {
    return ApiResponse.ok(
        Map.of(
            "has_update", false,
            "current_version", "0.1.0",
            "latest_version", "unsupported",
            "versions", List.of(),
            "update_check_status", "unsupported",
            "last_check_error", "No remote update source is configured"));
  }

  private String hostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception ignored) {
      return "unsupported";
    }
  }

  private String serverIp() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception ignored) {
      return "unsupported";
    }
  }

  private String databaseProductName() {
    return jdbcTemplate.execute(
        (org.springframework.jdbc.core.ConnectionCallback<String>)
            connection -> connection.getMetaData().getDatabaseProductName());
  }

  private long tableCount() {
    Number value =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where lower(table_schema) not in ('information_schema', 'pg_catalog')
            """,
            Number.class);
    return value == null ? 0 : value.longValue();
  }

  private String dbStatus() {
    Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
    return value != null && value == 1 ? "UP" : "DOWN";
  }

  private Object systemLoadAverage() {
    double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    return load < 0 ? "unsupported" : load;
  }
}
