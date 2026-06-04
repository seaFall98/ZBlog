package com.zblog.feedback.infrastructure;

import com.zblog.feedback.application.port.FeedbackMailer;
import com.zblog.mail.MailOutboxService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OutboxFeedbackMailer implements FeedbackMailer {

  private final MailOutboxService mailOutboxService;

  public OutboxFeedbackMailer(MailOutboxService mailOutboxService) {
    this.mailOutboxService = mailOutboxService;
  }

  public void sendNewFeedback(String ownerEmail, String ticketNo, Map<String, Object> formContent) {
    mailOutboxService.send(
        "admin",
        "feedback_new",
        ownerEmail,
        "New feedback " + ticketNo,
        "Ticket " + ticketNo + "\n" + formContent.get("description"));
  }

  public void sendReply(Map<String, Object> feedback, String recipient, String reply) {
    mailOutboxService.send(
        "user",
        "feedback_reply",
        recipient,
        "Feedback reply " + feedback.get("ticket_no"),
        "Ticket " + feedback.get("ticket_no") + "\n" + reply);
  }
}
