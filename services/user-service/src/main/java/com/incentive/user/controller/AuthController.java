package com.incentive.user.controller;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.dto.LoginRequest;
import com.incentive.user.dto.LoginResponse;
import com.incentive.user.dto.RegisterRequest;
import com.incentive.user.dto.UserResponse;
import com.incentive.user.config.RefreshTokenProperties;
import com.incentive.user.security.IssuedSession;
import com.incentive.user.support.UserBusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证", description = "提供注册、JWT 登录和 Refresh Token 轮换")
public class AuthController {
  private final UserAccountService service;
  private final RefreshTokenProperties refreshTokenProperties;
  /** 创建认证控制器。 */
  public AuthController(UserAccountService service, RefreshTokenProperties refreshTokenProperties) {
    this.service = service;
    this.refreshTokenProperties = refreshTokenProperties;
  }

  /** 接收用户注册请求。 */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "注册账户")
  public UserResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }

  /** 接收用户登录凭证校验请求。 */
  @PostMapping("/login")
  @Operation(summary = "校验登录凭证")
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    return writeSession(service.login(request), response);
  }

  @PostMapping("/refresh")
  @Operation(summary = "轮换 Refresh Token 并签发新的访问令牌")
  public LoginResponse refresh(
      @CookieValue(name = "${security.refresh-token.cookie-name}", required = false) String refreshToken,
      HttpServletResponse response) {
    try {
      return writeSession(service.refresh(refreshToken), response);
    } catch (UserBusinessException exception) {
      response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
      throw exception;
    }
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "退出登录并撤销 Refresh Token")
  public void logout(
      @CookieValue(name = "${security.refresh-token.cookie-name}", required = false) String refreshToken,
      HttpServletResponse response) {
    service.logout(refreshToken);
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
  }

  private LoginResponse writeSession(IssuedSession session, HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE,
        refreshCookie(session.refreshToken(),
            Duration.ofSeconds(session.refreshTokenExpiresInSeconds())).toString());
    return session.response();
  }

  private ResponseCookie refreshCookie(String value, Duration maxAge) {
    return ResponseCookie.from(refreshTokenProperties.cookieName(), value)
        .httpOnly(true)
        .secure(refreshTokenProperties.cookieSecure())
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(maxAge)
        .build();
  }
}
