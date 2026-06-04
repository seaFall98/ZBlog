package com.zblog.event;

import com.zblog.event.application.EventOutboxService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class EventOutboxScheduler {

  // 测试环境关闭真实调度，避免定时投递影响用例的确定性。
  private final EventOutboxService eventOutboxService;

  public EventOutboxScheduler(EventOutboxService eventOutboxService) {
    this.eventOutboxService = eventOutboxService;
  }

  @Scheduled(fixedDelayString = "${zblog.mq.publish-fixed-delay-ms:5000}")
  public void publishPending() {
    // 周期性 drain outbox，提供削峰和失败重试入口。
    eventOutboxService.publishPending();
  }
}
