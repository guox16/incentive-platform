package com.incentive.user.controller;

import com.incentive.user.dto.ChangeUserRoleRequest;
import com.incentive.user.dto.UserAuthorizationResponse;
import com.incentive.user.security.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users/admin")
@Tag(name = "用户权限管理")
public class AdminUserController {
  private final RbacService rbacService;

  public AdminUserController(RbacService rbacService) {
    this.rbacService = rbacService;
  }

  @PutMapping("/{userId}/role")
  @Operation(summary = "修改用户角色")
  public UserAuthorizationResponse changeRole(@PathVariable("userId") @Positive Long userId,
      @Valid @RequestBody ChangeUserRoleRequest request) {
    var authorization = rbacService.changeRole(userId, request.role());
    return new UserAuthorizationResponse(
        userId, authorization.role(), authorization.permissions());
  }
}
