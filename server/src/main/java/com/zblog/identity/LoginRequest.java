package com.zblog.identity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@JsonAlias("email") @NotBlank String username, @NotBlank String password) {}
