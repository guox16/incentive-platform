package com.incentive.activity.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.activity.application.DailyCheckInService;
import com.incentive.activity.dto.DailyCheckInResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DailyCheckInController.class)
class DailyCheckInControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private DailyCheckInService service;

  @Test
  void bindsAuthenticatedUserIdFromTrustedHeader() throws Exception {
    when(service.getStatus(1L)).thenReturn(new DailyCheckInResponse(
        1L, LocalDate.of(2026, 8, 7), false, 0, 10, "AVAILABLE",
        null, null, null, List.of()));

    mockMvc.perform(get("/api/v1/activities/check-ins/me")
            .header("X-User-Id", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.rewardStatus").value("AVAILABLE"));

    verify(service).getStatus(1L);
  }
}
