package com.incentive.user.controller;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户资料")
public class UserController {
  private final UserAccountService service;
  /** 创建用户资料控制器。 */
  public UserController(UserAccountService service) { this.service = service; }

  /** 查询指定用户的公开资料。 */
  @GetMapping("/me")
  @Operation(summary = "查询用户资料")
  public UserResponse getProfile(
      @RequestHeader("X-User-Id") @Positive Long userId) {
    return service.getProfile(userId);
  }

  /** 更新指定用户的昵称资料。 */
  @PutMapping("/me")
  @Operation(summary = "修改昵称")
  public UserResponse updateProfile(@RequestHeader("X-User-Id") @Positive Long userId,
      @Valid @RequestBody UpdateProfileRequest request) {
    return service.updateProfile(userId, request);
  }
}
