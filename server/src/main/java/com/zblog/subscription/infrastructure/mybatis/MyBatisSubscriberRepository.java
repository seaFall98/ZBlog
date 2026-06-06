package com.zblog.subscription.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.subscription.application.port.SubscriberRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSubscriberRepository implements SubscriberRepository {

  private final SubscriberMapper subscriberMapper;

  public MyBatisSubscriberRepository(SubscriberMapper subscriberMapper) {
    this.subscriberMapper = subscriberMapper;
  }

  public List<Long> findIdsByEmail(String email) {
    return subscriberMapper.idsByEmail(email);
  }

  public long create(String email, String unsubscribeToken) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("email", email);
    params.put("unsubscribeToken", unsubscribeToken);
    subscriberMapper.insertSubscriber(params);
    return ((Number) params.get("id")).longValue();
  }

  public void reactivate(long id) {
    subscriberMapper.reactivate(id);
  }

  public List<Long> findActiveIdsByToken(String token) {
    return subscriberMapper.activeIdsByToken(token);
  }

  public void deactivate(long id) {
    subscriberMapper.deactivate(id);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    return new PageResponse<>(
        subscriberMapper.listAdminRows(pageSize, offset), subscriberMapper.countActiveRows(), page, pageSize);
  }

  public void delete(long id) {
    subscriberMapper.delete(id);
  }

  public Map<String, Object> get(long id) {
    return subscriberMapper.rowsById(id).stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Subscriber not found", HttpStatus.NOT_FOUND));
  }
}
