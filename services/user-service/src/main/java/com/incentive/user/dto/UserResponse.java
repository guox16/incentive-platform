package com.incentive.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, String nickname, Instant createdAt, Instant updatedAt) {}
