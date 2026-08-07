package com.incentive.user.controller;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本阶段没有认证上下文，因此按 ID 查询/修改只可用于本地演示。
 * 接入 JWT 后必须改为从可信身份上下文取得用户 ID 并校验资源归属。
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户资料")
public class UserController {
  private final UserAccountService service;
  /** 创建用户资料控制器。 */
  public UserController(UserAccountService service) { this.service = service; }

  /** 查询指定用户的公开资料。 */
  @GetMapping("/{id}")
  @Operation(summary = "查询用户资料")
  public UserResponse getProfile(@PathVariable("id") Long id) { return service.getProfile(id); }

  /** 更新指定用户的昵称资料。 */
  @PutMapping("/{id}")
  @Operation(summary = "修改昵称")
  public UserResponse updateProfile(@PathVariable("id") Long id, @Valid @RequestBody UpdateProfileRequest request) {
    return service.updateProfile(id, request);
  }
}
