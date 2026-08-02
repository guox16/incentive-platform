package com.incentive.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$") String username,
    @NotBlank @Size(min = 6, max = 72) String password) {}

