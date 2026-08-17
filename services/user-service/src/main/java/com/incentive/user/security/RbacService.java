package com.incentive.user.security;

import com.incentive.user.domain.UserRole;
import com.incentive.user.domain.UserRoleAssignment;
import com.incentive.user.repository.RolePermissionRepository;
import com.incentive.user.repository.UserAccountRepository;
import com.incentive.user.repository.UserRoleAssignmentRepository;
import com.incentive.user.support.UserBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 将五表查询和角色变更规则隐藏在小型授权接口之后。 */
@Service
public class RbacService {
  private final UserAccountRepository users;
  private final UserRoleAssignmentRepository assignments;
  private final RolePermissionRepository grants;
  private final RefreshTokenService refreshTokens;

  public RbacService(UserAccountRepository users, UserRoleAssignmentRepository assignments,
      RolePermissionRepository grants, RefreshTokenService refreshTokens) {
    this.users = users;
    this.assignments = assignments;
    this.grants = grants;
    this.refreshTokens = refreshTokens;
  }

  @Transactional
  public void assignDefaultRole(Long userId) {
    assignments.save(new UserRoleAssignment(userId, UserRole.USER));
  }

  @Transactional
  public AuthorizationSnapshot authorizationFor(Long userId) {
    UserRoleAssignment assignment = assignments.findById(userId)
        .orElseGet(() -> assignments.save(new UserRoleAssignment(userId, UserRole.USER)));
    return snapshot(assignment.getRole());
  }

  @Transactional
  public AuthorizationSnapshot changeRole(Long userId, UserRole newRole) {
    if (!users.existsById(userId)) {
      throw new UserBusinessException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND);
    }
    UserRoleAssignment assignment = assignments.findById(userId)
        .orElseGet(() -> assignments.save(new UserRoleAssignment(userId, UserRole.USER)));
    if (assignment.getRole() == UserRole.SUPER_ADMIN
        && newRole != UserRole.SUPER_ADMIN
        && assignments.findAllByRoleForUpdate(UserRole.SUPER_ADMIN).size() <= 1) {
      throw new UserBusinessException(
          "LAST_SUPER_ADMIN", "不能降级系统中最后一个超级管理员", HttpStatus.CONFLICT);
    }
    assignment.changeRole(newRole);
    refreshTokens.revokeAllForUser(userId);
    return snapshot(newRole);
  }

  private AuthorizationSnapshot snapshot(UserRole role) {
    return new AuthorizationSnapshot(role, grants.findPermissionCodesByRole(role));
  }
}
