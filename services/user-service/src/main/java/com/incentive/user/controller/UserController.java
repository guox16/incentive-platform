package com.incentive.user.controller;

import com.incentive.user.application.UserAccountService;
import com.incentive.common.security.JwtUserId;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  public UserResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
    return service.getProfile(JwtUserId.from(jwt));
  }

  /** 更新指定用户的昵称资料。 */
  @PutMapping("/me")
  @Operation(summary = "修改昵称")
  public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateProfileRequest request) {
    return service.updateProfile(JwtUserId.from(jwt), request);
  }
}
