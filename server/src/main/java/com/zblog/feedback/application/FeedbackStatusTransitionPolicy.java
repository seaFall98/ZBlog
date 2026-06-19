package com.zblog.feedback.application;

import com.zblog.common.exception.BusinessException;
import com.zblog.feedback.domain.FeedbackStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FeedbackStatusTransitionPolicy {

  public void requireAllowed(FeedbackStatus current, FeedbackStatus target) {
    if (!current.canTransitionTo(target)) {
      throw new BusinessException(
          40070,
          "Invalid feedback status transition: " + current.name() + " -> " + target.name(),
          HttpStatus.BAD_REQUEST);
    }
  }

  public FeedbackStatus afterUserReply(FeedbackStatus current) {
    if (current == FeedbackStatus.CLOSED) {
      throw new BusinessException(40071, "Closed feedback cannot be replied to", HttpStatus.BAD_REQUEST);
    }
    if (current == FeedbackStatus.WAITING_USER) {
      return FeedbackStatus.IN_PROGRESS;
    }
    return current;
  }

  public boolean cleanupEligible(FeedbackStatus status) {
    return status == FeedbackStatus.RESOLVED || status == FeedbackStatus.CLOSED;
  }
}

