package com.incentive.user.dto;

import java.time.Instant;

public record UserResponse(Long id, String username, String phone, String nickname, Instant createdAt, Instant updatedAt) {}
