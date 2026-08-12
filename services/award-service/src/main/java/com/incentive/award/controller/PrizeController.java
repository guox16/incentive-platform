package com.incentive.award.controller;

import com.incentive.award.application.PrizeService;
import com.incentive.award.domain.PrizeStatus;
import com.incentive.award.dto.AdjustInventoryRequest;
import com.incentive.award.dto.CreatePrizeRequest;
import com.incentive.award.dto.PrizeInventoryLedgerResponse;
import com.incentive.award.dto.PrizeResponse;
import com.incentive.award.dto.UpdatePrizeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

/** 奖品管理接口；一期仅供受信管理端或接口调试工具调用。 */
@RestController
@RequestMapping("/api/v1/awards/prizes")
@Tag(name = "奖品管理")
public class PrizeController {
  private final PrizeService service;
  public PrizeController(PrizeService service) { this.service = service; }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "创建奖品")
  public PrizeResponse create(@Valid @RequestBody CreatePrizeRequest request) { return service.create(request); }
  @GetMapping("/{id}") @Operation(summary = "查询奖品")
  public PrizeResponse get(@PathVariable Long id) { return service.get(id); }
  @GetMapping @Operation(summary = "查询奖品列表")
  public List<PrizeResponse> list(@RequestParam(required = false) PrizeStatus status) { return service.list(status); }
  @PutMapping("/{id}") @Operation(summary = "修改奖品")
  public PrizeResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePrizeRequest request) { return service.update(id, request); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "软删除奖品")
  public void delete(@PathVariable Long id) { service.delete(id); }
  @PostMapping("/{id}/inventory-adjustments") @Operation(summary = "人工调整库存")
  public PrizeInventoryLedgerResponse adjustInventory(@PathVariable Long id, @Valid @RequestBody AdjustInventoryRequest request) { return service.adjustInventory(id, request); }
  @GetMapping("/{id}/inventory-ledgers") @Operation(summary = "查询库存流水")
  public List<PrizeInventoryLedgerResponse> inventoryLedgers(@PathVariable Long id) { return service.inventoryLedgers(id); }
}
