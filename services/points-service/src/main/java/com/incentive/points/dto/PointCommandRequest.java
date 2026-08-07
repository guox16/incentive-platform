package com.incentive.points.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PointCommandRequest(
    @NotNull UUID businessId,
    @NotNull @Positive Long userId,
    @Positive long amount,
    @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]*$") String source,
    @Size(max = 200) String remark) {}
