package com.incentive.points.controller;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.dto.PointBalanceResponse;
import com.incentive.points.dto.PointTransactionPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/points/me")
@Tag(name = "积分查询")
public class PointQueryController {
  private final PointAccountService service;
  /** 创建积分查询控制器。 */
  public PointQueryController(PointAccountService service) { this.service = service; }

  /** 查询指定用户的积分余额。 */
  @GetMapping("/balance")
  @Operation(summary = "查询积分余额")
  public PointBalanceResponse balance(@RequestHeader("X-User-Id") Long userId) {
    return service.getBalance(userId);
  }

  /** 分页查询指定用户的积分流水。 */
  @GetMapping("/transactions")
  @Operation(summary = "分页查询积分流水")
  public PointTransactionPageResponse transactions(
      @RequestHeader("X-User-Id") Long userId,
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.getTransactions(userId, page, size);
  }
}
