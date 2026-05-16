package com.zblog.mail;

public interface MailSender {

  void send(String recipient, String subject, String body);
}
