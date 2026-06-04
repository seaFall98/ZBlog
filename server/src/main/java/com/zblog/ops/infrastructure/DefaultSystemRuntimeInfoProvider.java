package com.zblog.ops.infrastructure;

import com.zblog.ops.application.port.SystemRuntimeInfoProvider;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DefaultSystemRuntimeInfoProvider implements SystemRuntimeInfoProvider {

  public Map<String, Object> staticInfo() {
    Runtime runtime = Runtime.getRuntime();
    File root = new File(".").getAbsoluteFile();
    return Map.ofEntries(
        Map.entry("cpu_core", runtime.availableProcessors()),
        Map.entry("cpu_model", System.getProperty("os.arch", "unsupported")),
        Map.entry("cpu_arch", System.getProperty("os.arch", "unsupported")),
        Map.entry("hostname", hostname()),
        Map.entry("os", System.getProperty("os.name", "unsupported")),
        Map.entry("server_ip", serverIp()),
        Map.entry("timezone", ZoneId.systemDefault().toString()),
        Map.entry("memory_total", runtime.maxMemory()),
        Map.entry("swap_total", "unsupported"),
        Map.entry("disk_total", root.getTotalSpace()),
        Map.entry("storage_status", "local"),
        Map.entry("email_status", "disabled"),
        Map.entry("feishu_status", "disabled"),
        Map.entry("app_version", "0.1.0"));
  }

  public Map<String, Object> dynamicInfo() {
    Runtime runtime = Runtime.getRuntime();
    File root = new File(".").getAbsoluteFile();
    long used = runtime.totalMemory() - runtime.freeMemory();
    return Map.ofEntries(
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
        Map.entry("db_size", "unsupported"),
        Map.entry("db_conn_count", "unsupported"),
        Map.entry("version_latest_version", "unsupported"),
        Map.entry("version_last_check_error", "update check unsupported in local backend"));
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

  private Object systemLoadAverage() {
    double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    return load < 0 ? "unsupported" : load;
  }
}
