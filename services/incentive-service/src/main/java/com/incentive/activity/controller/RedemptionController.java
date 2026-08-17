package com.incentive.activity.controller;

import com.incentive.activity.application.RedemptionService;
import com.incentive.common.security.JwtUserId;
import com.incentive.activity.dto.RedemptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/activities/redemptions")
@Tag(name = "兑换")
public class RedemptionController {
  private final RedemptionService service;

  public RedemptionController(RedemptionService service) {
    this.service = service;
  }

  @PostMapping("/{activityCode}/items/{itemId}")
  @Operation(summary = "兑换商品")
  public RedemptionResponse redeem(@PathVariable("activityCode") String activityCode,
      @PathVariable("itemId") @Positive Long itemId,
      @AuthenticationPrincipal Jwt jwt) {
    return service.redeem(activityCode, itemId, JwtUserId.from(jwt));
  }
}
