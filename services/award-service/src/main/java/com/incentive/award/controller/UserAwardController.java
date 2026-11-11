package com.incentive.award.controller;

import com.incentive.award.domain.AwardSourceType;
import com.incentive.award.domain.AwardType;
import com.incentive.award.repository.UserAwardRepository;
import com.incentive.common.security.JwtUserId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class UserAwardController {
  private final UserAwardRepository repository;

  public UserAwardController(UserAwardRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/api/v1/awards/me")
  public UserAwardPage findMyAwards(@AuthenticationPrincipal Jwt jwt,
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(50) int size) {
    var result = repository.findByUserIdOrderByObtainedAtDescIdDesc(
        JwtUserId.from(jwt), PageRequest.of(page, size));
    List<UserAwardItem> items = result.map(award -> new UserAwardItem(
        award.getId().toString(), award.getAwardName(), award.getAwardType(),
        award.getSourceType() == AwardSourceType.LOTTERY ? Source.LOTTERY : Source.REDEMPTION,
        award.getObtainedAt())).getContent();
    return new UserAwardPage(items, result.getNumber(), result.getTotalPages());
  }

  public record UserAwardPage(List<UserAwardItem> items, int page, int totalPages) {}

  public record UserAwardItem(
      String id, String name, AwardType type, Source source, Instant obtainedAt) {}

  public enum Source { LOTTERY, REDEMPTION }
}
