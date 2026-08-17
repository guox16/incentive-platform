package com.incentive.user.security;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import java.util.List;

public record AuthorizationSnapshot(UserRole role, List<PermissionCode> permissions) {
  public AuthorizationSnapshot {
    permissions = List.copyOf(permissions);
  }
}
