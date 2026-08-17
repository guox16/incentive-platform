package com.incentive.user.dto;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import java.util.List;

public record UserAuthorizationResponse(
    Long userId, UserRole role, List<PermissionCode> permissions) {}
