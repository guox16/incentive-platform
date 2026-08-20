package com.incentive.activity.controller;

import com.incentive.activity.application.LotteryService;
import com.incentive.activity.application.LotteryRecordQueryService;
import com.incentive.common.security.JwtUserId;
import com.incentive.activity.dto.LotteryDrawRequest;
import com.incentive.activity.dto.LotteryDrawResponse;
import com.incentive.activity.dto.LotteryRecordResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/activities/lotteries")
@Tag(name = "抽奖")
public class LotteryController {
  private final LotteryService service;
  private final LotteryRecordQueryService recordQueryService;

  public LotteryController(LotteryService service,
      LotteryRecordQueryService recordQueryService) {
    this.service = service;
    this.recordQueryService = recordQueryService;
  }

  @GetMapping("/orders/me")
  @Operation(summary = "查询本人最近抽奖记录")
  public List<LotteryRecordResponse> records(@AuthenticationPrincipal Jwt jwt) {
    return recordQueryService.findRecentByUser(JwtUserId.from(jwt));
  }

  @PostMapping("/{activityCode}/draw")
  @Operation(summary = "参与抽奖")
  public LotteryDrawResponse draw(@PathVariable("activityCode") String activityCode,
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LotteryDrawRequest request) {
    return service.draw(activityCode, JwtUserId.from(jwt), request.requestId());
  }
}
