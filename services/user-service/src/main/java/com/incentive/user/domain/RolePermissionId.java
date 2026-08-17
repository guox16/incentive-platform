package com.incentive.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolePermissionId implements Serializable {
  @Enumerated(EnumType.STRING)
  @Column(name = "role_code", length = 32)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "permission_code", length = 64)
  private PermissionCode permission;

  protected RolePermissionId() {}
  public RolePermissionId(UserRole role, PermissionCode permission) {
    this.role = role;
    this.permission = permission;
  }
  public UserRole getRole() { return role; }
  public PermissionCode getPermission() { return permission; }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof RolePermissionId that)) return false;
    return role == that.role && permission == that.permission;
  }
  @Override public int hashCode() { return Objects.hash(role, permission); }
}
