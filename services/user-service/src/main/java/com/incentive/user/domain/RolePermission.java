package com.incentive.user.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_permissions_role", columnList = "role_code"),
    @Index(name = "idx_role_permissions_permission", columnList = "permission_code")
})
public class RolePermission {
  @EmbeddedId
  private RolePermissionId id;

  protected RolePermission() {}
  public RolePermission(UserRole role, PermissionCode permission) {
    this.id = new RolePermissionId(role, permission);
  }
  public RolePermissionId getId() { return id; }
}
