package com.incentive.points.controller;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.dto.PointBalanceResponse;
import com.incentive.points.dto.PointTransactionPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/points/users/{userId}")
@Tag(name = "积分查询")
public class PointQueryController {
  private final PointAccountService service;
  public PointQueryController(PointAccountService service) { this.service = service; }

  @GetMapping("/balance")
  @Operation(summary = "查询积分余额")
  public PointBalanceResponse balance(@PathVariable UUID userId) { return service.getBalance(userId); }

  @GetMapping("/transactions")
  @Operation(summary = "分页查询积分流水")
  public PointTransactionPageResponse transactions(
      @PathVariable UUID userId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.getTransactions(userId, page, size);
  }
}
