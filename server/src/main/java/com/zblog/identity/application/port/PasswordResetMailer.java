package com.zblog.identity.application.port;

public interface PasswordResetMailer {

  void sendResetToken(String email, String token);
}
