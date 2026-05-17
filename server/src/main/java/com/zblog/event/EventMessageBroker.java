package com.zblog.event;

public interface EventMessageBroker {

  void publish(OutboxEvent event);
}
