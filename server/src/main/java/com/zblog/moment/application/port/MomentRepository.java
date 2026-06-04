package com.zblog.moment.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface MomentRepository {

  List<Map<String, Object>> findAll();

  Map<String, Object> find(long id);

  long create(String contentJson, boolean publish, Instant publishTime);

  void update(long id, String contentJson, boolean publish, Instant publishTime);

  void delete(long id);
}
