package com.incentive.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 15) String nickname,
    @NotBlank @Pattern(regexp = "^1\\d{10}$") String phone) {}
