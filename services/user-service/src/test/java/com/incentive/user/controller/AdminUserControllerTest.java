package com.incentive.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import com.incentive.user.security.AuthorizationSnapshot;
import com.incentive.user.security.RbacService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private RbacService rbacService;

  @Test
  void changesUserRoleAndReturnsEffectivePermissions() throws Exception {
    when(rbacService.changeRole(7L, UserRole.ADMIN)).thenReturn(new AuthorizationSnapshot(
        UserRole.ADMIN, List.of(PermissionCode.ACTIVITY_MANAGE, PermissionCode.PRIZE_MANAGE)));

    mockMvc.perform(put("/api/v1/users/admin/7/role")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.permissions[0]").value("ACTIVITY_MANAGE"));
  }
}
