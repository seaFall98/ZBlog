package com.zblog.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record LoginResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresInSeconds,
    Map<String, Object> user) {}
