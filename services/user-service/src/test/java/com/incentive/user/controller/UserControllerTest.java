package com.incentive.user.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.config.UserSecurityConfiguration;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(UserSecurityConfiguration.class)
class UserControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private UserAccountService service;
  @MockBean private StringRedisTemplate redis;

  @Test
  void getsProfileUsingVerifiedJwtSubjectAndIgnoresSpoofedHeader() throws Exception {
    Long id = 1L;
    when(service.getProfile(id)).thenReturn(new UserResponse(id, "user", "13800138000", "name", null, null));

    mockMvc.perform(get("/api/v1/users/me")
            .with(jwt().jwt(token -> token.subject("1"))
                .authorities(new SimpleGrantedAuthority("ACCOUNT_SELF")))
            .header("X-User-Id", "999"))
        .andExpect(status().isOk());

    org.mockito.Mockito.verify(service).getProfile(1L);
  }

  @Test
  void updatesProfileUsingVerifiedJwtSubject() throws Exception {
    Long id = 1L;
    when(service.updateProfile(eq(id), any(UpdateProfileRequest.class)))
        .thenReturn(new UserResponse(id, "user", "13800138000", "new-name", null, null));

    mockMvc.perform(put("/api/v1/users/me")
            .with(jwt().jwt(token -> token.subject("1"))
                .authorities(new SimpleGrantedAuthority("ACCOUNT_SELF")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"new-name\",\"phone\":\"13900139000\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsNicknameLongerThanFifteenCharacters() throws Exception {
    mockMvc.perform(put("/api/v1/users/me")
            .with(jwt().jwt(token -> token.subject("1"))
                .authorities(new SimpleGrantedAuthority("ACCOUNT_SELF")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"sixteen-character\",\"phone\":\"13900139000\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsIdentityHeaderWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/users/me").header("X-User-Id", "1"))
        .andExpect(status().isUnauthorized());
  }
}
