package com.incentive.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleDefinition {
  @Id
  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private UserRole code;
  @Column(nullable = false, length = 64)
  private String name;

  protected RoleDefinition() {}
  public RoleDefinition(UserRole code, String name) { this.code = code; this.name = name; }
  public UserRole getCode() { return code; }
}
