package com.incentive.user.security;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.PermissionDefinition;
import com.incentive.user.domain.RoleDefinition;
import com.incentive.user.domain.RolePermission;
import com.incentive.user.domain.UserRole;
import com.incentive.user.repository.PermissionDefinitionRepository;
import com.incentive.user.repository.RoleDefinitionRepository;
import com.incentive.user.repository.RolePermissionRepository;
import java.util.EnumSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 为新库及旧开发库补齐稳定的 RBAC 目录；已有授权关系不会被启动过程覆盖。 */
@Component
public class RbacCatalogInitializer implements ApplicationRunner {
  private final RoleDefinitionRepository roles;
  private final PermissionDefinitionRepository permissions;
  private final RolePermissionRepository grants;

  public RbacCatalogInitializer(RoleDefinitionRepository roles,
      PermissionDefinitionRepository permissions, RolePermissionRepository grants) {
    this.roles = roles;
    this.permissions = permissions;
    this.grants = grants;
  }

  @Override
  public void run(ApplicationArguments args) {
    saveRole(UserRole.USER, "用户");
    saveRole(UserRole.ADMIN, "管理员");
    saveRole(UserRole.SUPER_ADMIN, "超级管理员");
    for (PermissionCode permission : PermissionCode.values()) {
      if (!permissions.existsById(permission)) {
        permissions.save(new PermissionDefinition(permission, permissionName(permission)));
      }
    }
    if (grants.count() == 0) seedDefaultGrants();
  }

  private void saveRole(UserRole role, String name) {
    if (!roles.existsById(role)) roles.save(new RoleDefinition(role, name));
  }

  private void seedDefaultGrants() {
    grant(UserRole.USER, EnumSet.of(
        PermissionCode.ACCOUNT_SELF, PermissionCode.POINTS_SELF, PermissionCode.CHECK_IN,
        PermissionCode.LOTTERY_PARTICIPATE, PermissionCode.REDEMPTION_PARTICIPATE));
    grant(UserRole.ADMIN, EnumSet.of(
        PermissionCode.ACCOUNT_SELF, PermissionCode.ACTIVITY_MANAGE,
        PermissionCode.PRIZE_MANAGE, PermissionCode.INVENTORY_MANAGE));
    grant(UserRole.SUPER_ADMIN, EnumSet.allOf(PermissionCode.class));
  }

  private void grant(UserRole role, EnumSet<PermissionCode> permissionCodes) {
    grants.saveAll(permissionCodes.stream()
        .map(permission -> new RolePermission(role, permission)).toList());
  }

  private String permissionName(PermissionCode permission) {
    return switch (permission) {
      case ACCOUNT_SELF -> "查看和修改本人资料";
      case POINTS_SELF -> "查看本人积分";
      case CHECK_IN -> "参与签到";
      case LOTTERY_PARTICIPATE -> "参与抽奖";
      case REDEMPTION_PARTICIPATE -> "参与兑换";
      case ACTIVITY_MANAGE -> "管理活动";
      case PRIZE_MANAGE -> "管理奖品";
      case INVENTORY_MANAGE -> "管理库存";
      case ROLE_MANAGE -> "管理用户角色";
    };
  }
}
