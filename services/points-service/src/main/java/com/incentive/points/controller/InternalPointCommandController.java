package com.incentive.points.controller;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.dto.PointCommandRequest;
import com.incentive.points.dto.PointTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部积分命令当前仅依赖受信网络隔离；后续必须增加服务身份或请求签名。
 * 网关不会为该路径配置外部路由。
 */
@RestController
@RequestMapping("/api/v1/internal/points")
@Tag(name = "内部积分命令", description = "仅供受信服务调用")
public class InternalPointCommandController {
  private final PointAccountService service;
  /** 创建内部积分命令控制器。 */
  public InternalPointCommandController(PointAccountService service) { this.service = service; }

  /** 接收内部服务的积分增加命令。 */
  @PostMapping("/credit")
  @Operation(summary = "增加积分")
  public PointTransactionResponse credit(@Valid @RequestBody PointCommandRequest request) {
    return service.credit(request);
  }

  /** 接收内部服务的积分扣减命令。 */
  @PostMapping("/debit")
  @Operation(summary = "扣减积分")
  public PointTransactionResponse debit(@Valid @RequestBody PointCommandRequest request) {
    return service.debit(request);
  }
}
