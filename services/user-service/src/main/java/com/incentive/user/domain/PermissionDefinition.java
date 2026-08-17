package com.incentive.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class PermissionDefinition {
  @Id
  @Enumerated(EnumType.STRING)
  @Column(length = 64)
  private PermissionCode code;
  @Column(nullable = false, length = 128)
  private String name;

  protected PermissionDefinition() {}
  public PermissionDefinition(PermissionCode code, String name) { this.code = code; this.name = name; }
  public PermissionCode getCode() { return code; }
}
