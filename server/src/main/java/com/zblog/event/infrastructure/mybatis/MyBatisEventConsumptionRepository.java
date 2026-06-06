package com.zblog.event.infrastructure.mybatis;

import com.zblog.event.application.port.EventConsumptionRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisEventConsumptionRepository implements EventConsumptionRepository {

  private final EventConsumptionMapper eventConsumptionMapper;

  public MyBatisEventConsumptionRepository(EventConsumptionMapper eventConsumptionMapper) {
    this.eventConsumptionMapper = eventConsumptionMapper;
  }

  public boolean alreadyConsumed(long eventId, String consumerName) {
    return eventConsumptionMapper.countConsumed(eventId, consumerName) > 0;
  }

  public void markConsumed(long eventId, String consumerName) {
    eventConsumptionMapper.insertConsumed(eventId, consumerName);
  }
}
