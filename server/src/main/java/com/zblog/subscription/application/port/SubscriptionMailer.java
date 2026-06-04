package com.zblog.subscription.application.port;

import java.util.Map;

public interface SubscriptionMailer {

  void sendSubscribeConfirm(Map<String, Object> subscriber);

  void sendUnsubscribeConfirm(Map<String, Object> subscriber);
}
