package com.incentive.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.dto.LoginRequest;
import com.incentive.user.dto.LoginResponse;
import com.incentive.user.dto.UserResponse;
import com.incentive.user.config.RefreshTokenProperties;
import com.incentive.user.config.UserSecurityConfiguration;
import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import com.incentive.user.security.IssuedSession;
import com.incentive.user.security.AccessTokenBlacklistService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

@WebMvcTest(AuthController.class)
@Import(UserSecurityConfiguration.class)
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private UserAccountService service;
  @MockBean private RefreshTokenProperties refreshTokenProperties;
  @MockBean private AccessTokenBlacklistService accessTokenBlacklist;
  @MockBean private StringRedisTemplate redis;

  @BeforeEach
  void setUp() {
    when(refreshTokenProperties.cookieName()).thenReturn("refresh_token");
    when(refreshTokenProperties.cookieSecure()).thenReturn(false);
  }

  @Test
  void rejectsInvalidRegistrationParametersWithUnifiedError() throws Exception {
    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void returnsAccessTokenAfterSuccessfulLogin() throws Exception {
    when(service.login(new LoginRequest("alice", "secret12"))).thenReturn(
        new IssuedSession(
            new LoginResponse("signed-token", "Bearer", 900, UserRole.USER,
                List.of(PermissionCode.ACCOUNT_SELF, PermissionCode.POINTS_SELF),
                new UserResponse(1L, "alice", "13800138000", "Alice", null, null)),
            "raw-refresh-token", 2592000));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"alice\",\"password\":\"secret12\"}"))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("refresh_token=raw-refresh-token"),
                org.hamcrest.Matchers.containsString("HttpOnly"),
                org.hamcrest.Matchers.containsString("SameSite=Strict"))))
        .andExpect(jsonPath("$.accessToken").value("signed-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.permissions[0]").value("ACCOUNT_SELF"))
        .andExpect(jsonPath("$.user.id").value(1));
  }

  @Test
  void rotatesRefreshTokenFromCookie() throws Exception {
    when(service.refresh("old-refresh")).thenReturn(new IssuedSession(
        new LoginResponse("new-access", "Bearer", 900, UserRole.USER,
            List.of(PermissionCode.ACCOUNT_SELF),
            new UserResponse(1L, "alice", "13800138000", "Alice", null, null)),
        "new-refresh", 2592000));

    mockMvc.perform(post("/api/v1/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=new-refresh")));
  }

  @Test
  void revokesRefreshTokenAndExpiresCookieOnLogout() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout")
            .with(jwt().jwt(token -> token.subject("1").claim("jti", "access-jti")))
            .cookie(new jakarta.servlet.http.Cookie("refresh_token", "current-refresh")))
        .andExpect(status().isNoContent())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("refresh_token="),
                org.hamcrest.Matchers.containsString("Max-Age=0"))));

    verify(service).logout("current-refresh");
    verify(accessTokenBlacklist).revoke(org.mockito.ArgumentMatchers.any(Jwt.class));
  }
}
