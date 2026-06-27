package com.zblog.site.infrastructure.mybatis;

import com.zblog.site.application.port.SettingRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
                row -> Objects.toString(row.get("value_text"), ""),
                (left, right) -> right,
                LinkedHashMap::new));
  }

  public void upsert(String group, String key, String value) {
    int updated = settingMapper.updateValue(group, key, value);
    if (updated == 0) {
      settingMapper.insertValue(group, key, value);
    }
  }

  public void deleteKey(String group, String key) {
    settingMapper.deleteKey(group, key);
  }

  public int clearValuesEqualTo(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    return settingMapper.clearValuesEqualTo(value);
  }

  public void deleteGroup(String group) {
    settingMapper.deleteGroup(group);
  }
}
