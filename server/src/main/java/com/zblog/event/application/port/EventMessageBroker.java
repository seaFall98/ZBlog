package com.zblog.event.application.port;

import com.zblog.event.domain.OutboxEvent;

public interface EventMessageBroker {

  void publish(OutboxEvent event);
}
