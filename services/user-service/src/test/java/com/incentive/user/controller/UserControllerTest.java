package com.incentive.user.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.user.application.UserAccountService;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private UserAccountService service;

  @Test
  void getsProfileUsingTheTrustedIdentityHeader() throws Exception {
    Long id = 1L;
    when(service.getProfile(id)).thenReturn(new UserResponse(id, "user", "13800138000", "name", null, null));

    mockMvc.perform(get("/api/v1/users/me").header("X-User-Id", id))
        .andExpect(status().isOk());
  }

  @Test
  void updatesProfileUsingTheTrustedIdentityHeader() throws Exception {
    Long id = 1L;
    when(service.updateProfile(eq(id), any(UpdateProfileRequest.class)))
        .thenReturn(new UserResponse(id, "user", "13800138000", "new-name", null, null));

    mockMvc.perform(put("/api/v1/users/me")
            .header("X-User-Id", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"new-name\",\"phone\":\"13900139000\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsNicknameLongerThanFifteenCharacters() throws Exception {
    mockMvc.perform(put("/api/v1/users/me")
            .header("X-User-Id", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"sixteen-character\",\"phone\":\"13900139000\"}"))
        .andExpect(status().isBadRequest());
  }
}
