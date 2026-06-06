package com.zblog.moment.infrastructure.mybatis;

import com.zblog.common.exception.BusinessException;
import com.zblog.moment.application.port.MomentRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisMomentRepository implements MomentRepository {

  private final MomentMapper momentMapper;

  public MyBatisMomentRepository(MomentMapper momentMapper) {
    this.momentMapper = momentMapper;
  }

  public List<Map<String, Object>> findAll() {
    return momentMapper.findAll();
  }

  public Map<String, Object> find(long id) {
    List<Map<String, Object>> rows = momentMapper.rowsById(id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Moment not found", HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  public long create(String contentJson, boolean publish, Instant publishTime) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("contentJson", contentJson);
    params.put("publish", publish);
    params.put("publishTime", publishTime);
    momentMapper.insertMoment(params);
    return ((Number) params.get("id")).longValue();
  }

  public void update(long id, String contentJson, boolean publish, Instant publishTime) {
    momentMapper.updateMoment(id, contentJson, publish, publishTime);
  }

  public void delete(long id) {
    momentMapper.deleteMoment(id);
  }
}
