package com.zblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zblog.security")
public class SecurityProperties {

  private String jwtSecret;
  private long tokenTtlMinutes = 120;
  private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public long getTokenTtlMinutes() {
    return tokenTtlMinutes;
  }

  public void setTokenTtlMinutes(long tokenTtlMinutes) {
    this.tokenTtlMinutes = tokenTtlMinutes;
  }

  public BootstrapAdmin getBootstrapAdmin() {
    return bootstrapAdmin;
  }

  public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
    this.bootstrapAdmin = bootstrapAdmin;
  }

  public static class BootstrapAdmin {
    private String username;
    private String password;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
