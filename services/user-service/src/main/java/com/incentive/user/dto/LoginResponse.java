package com.incentive.user.dto;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import java.util.List;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserRole role,
    List<PermissionCode> permissions,
    UserResponse user) {}
