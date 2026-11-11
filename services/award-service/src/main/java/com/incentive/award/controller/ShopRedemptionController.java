package com.incentive.award.controller;

import com.incentive.award.application.ShopRedemptionService;
import com.incentive.award.dto.ShopRedemptionResponse;
import com.incentive.common.security.JwtUserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/awards/redemptions")
public class ShopRedemptionController {
  private final ShopRedemptionService service;

  public ShopRedemptionController(ShopRedemptionService service) {
    this.service = service;
  }

  @GetMapping
  public List<ShopRedemptionResponse> recent(@AuthenticationPrincipal Jwt jwt) {
    return service.recent(JwtUserId.from(jwt));
  }

  @PostMapping("/{awardId}")
  public ShopRedemptionResponse redeem(@PathVariable("awardId") @Positive Long awardId,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) String requestId,
      @Valid @RequestBody RedeemRequest request, @AuthenticationPrincipal Jwt jwt) {
    return service.redeem(JwtUserId.from(jwt), requestId, awardId, request.expectedPoints());
  }

  public record RedeemRequest(@NotNull @Positive Long expectedPoints) {}
}
