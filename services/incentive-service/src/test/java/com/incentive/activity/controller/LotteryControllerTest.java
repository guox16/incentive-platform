package com.incentive.activity.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.activity.application.LotteryService;
import com.incentive.activity.config.IncentiveSecurityConfiguration;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryDrawResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotteryController.class)
@Import(IncentiveSecurityConfiguration.class)
class LotteryControllerTest {
  private static final Instant DRAWN_AT = Instant.parse("2026-08-20T00:30:00Z");
  @Autowired private MockMvc mockMvc;
  @MockBean private LotteryService service;
  @MockBean private StringRedisTemplate redis;

  @Test
  void returnsCompletedLotteryResultToFrontend() throws Exception {
    when(service.draw("SUMMER_LOTTERY", 7L, "request-1"))
        .thenReturn(new LotteryDrawResponse(
            51L, "SUMMER_LOTTERY", 7L, 131L, "优惠券", PrizeType.VIRTUAL,
            "https://example.com/prize.png", true, true, 10L, 44L, 90L, DRAWN_AT));

    mockMvc.perform(post("/api/v1/activities/lotteries/SUMMER_LOTTERY/draw")
            .with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("LOTTERY_PARTICIPATE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"requestId\":\"request-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participationId").value(51))
        .andExpect(jsonPath("$.activityCode").value("SUMMER_LOTTERY"))
        .andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.prizeId").value(131))
        .andExpect(jsonPath("$.prizeName").value("优惠券"))
        .andExpect(jsonPath("$.prizeType").value("VIRTUAL"))
        .andExpect(jsonPath("$.won").value(true))
        .andExpect(jsonPath("$.pendingAwardCreated").value(true))
        .andExpect(jsonPath("$.pointsCost").value(10))
        .andExpect(jsonPath("$.pointTransactionId").value(44))
        .andExpect(jsonPath("$.balanceAfter").value(90))
        .andExpect(jsonPath("$.drawnAt").value("2026-08-20T00:30:00Z"));

    verify(service).draw("SUMMER_LOTTERY", 7L, "request-1");
  }

  @Test
  void rejectsMissingRequestIdBeforeStartingLottery() throws Exception {
    mockMvc.perform(post("/api/v1/activities/lotteries/SUMMER_LOTTERY/draw")
            .with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("LOTTERY_PARTICIPATE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }
}
