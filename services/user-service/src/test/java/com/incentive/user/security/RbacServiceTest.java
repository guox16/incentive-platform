package com.incentive.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import com.incentive.user.domain.UserRoleAssignment;
import com.incentive.user.repository.RolePermissionRepository;
import com.incentive.user.repository.UserAccountRepository;
import com.incentive.user.repository.UserRoleAssignmentRepository;
import com.incentive.user.support.UserBusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {
  @Mock private UserAccountRepository users;
  @Mock private UserRoleAssignmentRepository assignments;
  @Mock private RolePermissionRepository grants;
  @Mock private RefreshTokenService refreshTokens;
  private RbacService service;

  @BeforeEach
  void setUp() {
    service = new RbacService(users, assignments, grants, refreshTokens);
  }

  @Test
  void loadsRoleAndPermissionsAsOneAuthorizationSnapshot() {
    when(assignments.findById(7L))
        .thenReturn(Optional.of(new UserRoleAssignment(7L, UserRole.ADMIN)));
    when(grants.findPermissionCodesByRole(UserRole.ADMIN))
        .thenReturn(List.of(PermissionCode.ACCOUNT_SELF, PermissionCode.ACTIVITY_MANAGE));

    AuthorizationSnapshot result = service.authorizationFor(7L);

    assertThat(result.role()).isEqualTo(UserRole.ADMIN);
    assertThat(result.permissions())
        .containsExactly(PermissionCode.ACCOUNT_SELF, PermissionCode.ACTIVITY_MANAGE);
  }

  @Test
  void changesRoleAndRevokesAllRefreshTokens() {
    UserRoleAssignment assignment = new UserRoleAssignment(7L, UserRole.USER);
    when(users.existsById(7L)).thenReturn(true);
    when(assignments.findById(7L)).thenReturn(Optional.of(assignment));
    when(grants.findPermissionCodesByRole(UserRole.ADMIN))
        .thenReturn(List.of(PermissionCode.ACTIVITY_MANAGE));

    AuthorizationSnapshot result = service.changeRole(7L, UserRole.ADMIN);

    assertThat(assignment.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(result.permissions()).containsExactly(PermissionCode.ACTIVITY_MANAGE);
    verify(refreshTokens).revokeAllForUser(7L);
  }

  @Test
  void refusesToDemoteLastSuperAdmin() {
    when(users.existsById(1L)).thenReturn(true);
    when(assignments.findById(1L))
        .thenReturn(Optional.of(new UserRoleAssignment(1L, UserRole.SUPER_ADMIN)));
    when(assignments.findAllByRoleForUpdate(UserRole.SUPER_ADMIN))
        .thenReturn(List.of(new UserRoleAssignment(1L, UserRole.SUPER_ADMIN)));

    assertThatThrownBy(() -> service.changeRole(1L, UserRole.ADMIN))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("LAST_SUPER_ADMIN");
    verifyNoInteractions(refreshTokens);
  }
}
