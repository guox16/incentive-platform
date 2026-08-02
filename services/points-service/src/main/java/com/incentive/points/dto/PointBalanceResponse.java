package com.incentive.points.dto;

import java.time.Instant;
import java.util.UUID;

public record PointBalanceResponse(UUID userId, long balance, boolean accountCreated, Instant updatedAt) {}
