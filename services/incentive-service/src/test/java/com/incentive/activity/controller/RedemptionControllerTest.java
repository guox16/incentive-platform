package com.incentive.activity.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.activity.application.RedemptionService;
import com.incentive.activity.config.IncentiveSecurityConfiguration;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.RedemptionResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RedemptionController.class)
@Import(IncentiveSecurityConfiguration.class)
class RedemptionControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private RedemptionService service;
  @MockBean private StringRedisTemplate redis;

  @Test
  void passesIdempotencyKeyAndAuthenticatedUserToService() throws Exception {
    when(service.redeem("POINTS_MALL", 10L, 7L, "request-123"))
        .thenReturn(new RedemptionResponse(71L, "POINTS_MALL", 10L, "WELCOME_COUPON",
            7L, 101L, "新人优惠券", PrizeType.VIRTUAL, null, 50L, 61L, 150L,
            true, Instant.parse("2026-08-11T12:00:00Z")));

    mockMvc.perform(post("/api/v1/activities/redemptions/POINTS_MALL/items/10")
            .with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("REDEMPTION_PARTICIPATE")))
            .header("Idempotency-Key", "request-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.redemptionId").value(71))
        .andExpect(jsonPath("$.balanceAfter").value(150));

    verify(service).redeem("POINTS_MALL", 10L, 7L, "request-123");
  }

  @Test
  void rejectsRequestWithoutIdempotencyKey() throws Exception {
    mockMvc.perform(post("/api/v1/activities/redemptions/POINTS_MALL/items/10")
            .with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("REDEMPTION_PARTICIPATE"))))
        .andExpect(status().isBadRequest());
  }
}
