package com.incentive.activity.controller;

import com.incentive.activity.application.DailyCheckInService;
import com.incentive.common.security.JwtUserId;
import com.incentive.activity.dto.DailyCheckInResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/activities/check-ins/me")
@Tag(name = "每日签到")
public class DailyCheckInController {
  private final DailyCheckInService service;

  public DailyCheckInController(DailyCheckInService service) { this.service = service; }

  @GetMapping
  @Operation(summary = "查询今日签到状态")
  public DailyCheckInResponse status(@AuthenticationPrincipal Jwt jwt) {
    return service.getStatus(JwtUserId.from(jwt));
  }

  @PostMapping
  @Operation(summary = "完成今日签到")
  public DailyCheckInResponse checkIn(@AuthenticationPrincipal Jwt jwt,
      @RequestHeader("Idempotency-Key")
      @NotBlank @Size(max = 64)
      @Pattern(regexp = "[A-Za-z0-9._:-]+") String requestId) {
    return service.checkIn(JwtUserId.from(jwt), requestId.trim());
  }
}
