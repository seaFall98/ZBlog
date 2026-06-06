package com.zblog.site.infrastructure.mybatis;

import com.zblog.site.application.port.SettingRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSettingRepository implements SettingRepository {

  private final SettingMapper settingMapper;

  public MyBatisSettingRepository(SettingMapper settingMapper) {
    this.settingMapper = settingMapper;
  }

  public Map<String, String> getGroup(String group) {
    return settingMapper.groupRows(group).stream()
        .collect(
            Collectors.toMap(
                row -> row.get("key_name").toString(),
                row -> row.get("value_text").toString(),
                (left, right) -> right,
                LinkedHashMap::new));
  }

  public void upsert(String group, String key, String value) {
    int updated = settingMapper.updateValue(group, key, value);
    if (updated == 0) {
      settingMapper.insertValue(group, key, value);
    }
  }
}
