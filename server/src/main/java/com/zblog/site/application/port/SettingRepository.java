package com.zblog.site.application.port;

import java.util.Map;

public interface SettingRepository {

  Map<String, String> getGroup(String group);

  void upsert(String group, String key, String value);

  void deleteKey(String group, String key);

  default int clearValuesEqualTo(String value) {
    return 0;
  }

  void deleteGroup(String group);
}
