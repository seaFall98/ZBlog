package com.zblog.event.application.port;

public interface EventConsumptionRepository {

  boolean alreadyConsumed(long eventId, String consumerName);

  void markConsumed(long eventId, String consumerName);
}
