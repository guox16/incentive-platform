package com.incentive.points.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.application.PointReservationService;
import com.incentive.points.config.PointsSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.redis.core.StringRedisTemplate;

@WebMvcTest(InternalPointCommandController.class)
@Import(PointsSecurityConfiguration.class)
class PointControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private PointAccountService service;
  @MockBean private PointReservationService reservationService;
  @MockBean private StringRedisTemplate redis;

  @Test
  void rejectsInvalidCreditCommandWithUnifiedError() throws Exception {
    mockMvc.perform(post("/api/v1/internal/points/credit")
            .with(jwt().authorities(new SimpleGrantedAuthority("POINTS_COMMAND")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsOrdinaryUserJwtOnInternalPointCommand() throws Exception {
    mockMvc.perform(post("/api/v1/internal/points/credit")
            .with(jwt().jwt(token -> token.subject("1"))
                .authorities(new SimpleGrantedAuthority("POINTS_SELF")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"businessId\":1,\"userId\":1,\"amount\":10,\"source\":\"TEST\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsInvalidReservationCommand() throws Exception {
    mockMvc.perform(post("/api/v1/internal/points/reservations")
            .with(jwt().authorities(new SimpleGrantedAuthority("POINTS_COMMAND")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"businessId\":1,\"userId\":1,\"amount\":10,\"source\":\"TEST\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
