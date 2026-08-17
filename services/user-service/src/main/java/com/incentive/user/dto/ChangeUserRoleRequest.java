package com.incentive.user.dto;

import com.incentive.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(@NotNull UserRole role) {}
