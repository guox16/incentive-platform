package com.incentive.points.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.application.PointReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalPointCommandControllerBindingTest {
  private PointReservationService reservationService;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    reservationService = mock(PointReservationService.class);
    mvc = MockMvcBuilders.standaloneSetup(new InternalPointCommandController(
        mock(PointAccountService.class), reservationService)).build();
  }

  @Test
  void bindsBusinessIdForReservationOperations() throws Exception {
    mvc.perform(post("/api/v1/internal/points/reservations/123/confirm"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/internal/points/reservations/123/cancel"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/internal/points/reservations/123"))
        .andExpect(status().isOk());

    verify(reservationService).confirm(123L);
    verify(reservationService).cancel(123L);
    verify(reservationService).get(123L);
  }
}
