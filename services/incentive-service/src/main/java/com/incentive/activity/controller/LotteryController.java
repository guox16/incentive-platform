package com.incentive.activity.controller;

import com.incentive.activity.application.LotteryService;
import com.incentive.activity.dto.LotteryDrawResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/activities/lotteries")
@Tag(name = "抽奖")
public class LotteryController {
  private final LotteryService service;

  public LotteryController(LotteryService service) {
    this.service = service;
  }

  @PostMapping("/{activityCode}/users/{userId}/draw")
  @Operation(summary = "参与抽奖")
  public LotteryDrawResponse draw(@PathVariable("activityCode") String activityCode,
      @PathVariable("userId") @Positive Long userId) {
    return service.draw(activityCode, userId);
  }
}
