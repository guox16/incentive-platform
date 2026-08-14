package com.incentive.activity.dto;

import com.incentive.activity.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateActivityRequest(
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Z0-9_]+") String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull ActivityType type,
    @NotNull Instant startsAt,
    Instant endsAt,
    @PositiveOrZero long pointsCost,
    @Positive Integer dailyLimit,
    String qualificationRule) {}
