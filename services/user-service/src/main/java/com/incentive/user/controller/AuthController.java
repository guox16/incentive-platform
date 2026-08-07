package com.incentive.user.controller;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.dto.LoginRequest;
import com.incentive.user.dto.RegisterRequest;
import com.incentive.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证", description = "本阶段只提供注册与无状态登录校验")
public class AuthController {
  private final UserAccountService service;
  /** 创建认证控制器。 */
  public AuthController(UserAccountService service) { this.service = service; }

  /** 接收用户注册请求。 */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "注册账户")
  public UserResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }

  /** 接收用户登录凭证校验请求。 */
  @PostMapping("/login")
  @Operation(summary = "校验登录凭证")
  public UserResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
}
