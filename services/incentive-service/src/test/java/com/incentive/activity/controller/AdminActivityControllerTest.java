package com.incentive.activity.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.activity.application.AdminActivityService;
import com.incentive.activity.application.AdminPrizePoolService;
import com.incentive.activity.dto.AdminPrizePoolResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminActivityControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private AdminActivityService activityService;
  @MockBean private AdminPrizePoolService prizePoolService;

  @Test
  void bindsPrizePoolActivityIdWithoutCompilerParameterMetadata() throws Exception {
    when(prizePoolService.get(5L, "Bearer test-token"))
        .thenReturn(new AdminPrizePoolResponse(List.of(), List.of()));

    mockMvc.perform(get("/api/v1/activities/admin/5/prize-pool")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").isArray())
        .andExpect(jsonPath("$.candidates").isArray());

    verify(prizePoolService).get(5L, "Bearer test-token");
  }

  @Test
  void deletesDraftActivity() throws Exception {
    mockMvc.perform(delete("/api/v1/activities/admin/5"))
        .andExpect(status().isNoContent());

    verify(activityService).delete(5L);
  }
}
