package com.incentive.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles", indexes = @Index(name = "idx_user_roles_role", columnList = "role"))
public class UserRoleAssignment {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 32)
  private UserRole role;

  protected UserRoleAssignment() {}
  public UserRoleAssignment(Long userId, UserRole role) { this.userId = userId; this.role = role; }
  public Long getUserId() { return userId; }
  public UserRole getRole() { return role; }
  public void changeRole(UserRole role) { this.role = role; }
}
