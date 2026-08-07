package com.incentive.points.dto;

import java.time.Instant;

public record PointBalanceResponse(Long userId, long balance, boolean accountCreated, Instant updatedAt) {}
