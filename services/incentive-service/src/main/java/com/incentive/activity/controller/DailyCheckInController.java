package com.incentive.activity.controller;

import com.incentive.activity.application.DailyCheckInService;
import com.incentive.activity.dto.DailyCheckInResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/activities/check-ins/users/{userId}")
@Tag(name = "每日签到")
public class DailyCheckInController {
  private final DailyCheckInService service;

  public DailyCheckInController(DailyCheckInService service) { this.service = service; }

  @GetMapping
  @Operation(summary = "查询今日签到状态")
  public DailyCheckInResponse status(@PathVariable("userId") @Positive Long userId) {
    return service.getStatus(userId);
  }

  @PostMapping
  @Operation(summary = "完成今日签到")
  public DailyCheckInResponse checkIn(@PathVariable("userId") @Positive Long userId) {
    return service.checkIn(userId);
  }
}
