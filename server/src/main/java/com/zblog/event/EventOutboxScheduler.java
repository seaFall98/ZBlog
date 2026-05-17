package com.zblog.event;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class EventOutboxScheduler {

  private final EventOutboxService eventOutboxService;

  public EventOutboxScheduler(EventOutboxService eventOutboxService) {
    this.eventOutboxService = eventOutboxService;
  }

  @Scheduled(fixedDelayString = "${zblog.mq.publish-fixed-delay-ms:5000}")
  public void publishPending() {
    eventOutboxService.publishPending();
  }
}
