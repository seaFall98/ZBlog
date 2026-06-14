package com.zblog.scheduler;

import java.util.Map;

public interface ScheduledJobHandler {

  String name();

  String description();

  default Map<String, Object> defaultParameters() {
    return Map.of();
  }

  String execute(Map<String, Object> parameters);
}
