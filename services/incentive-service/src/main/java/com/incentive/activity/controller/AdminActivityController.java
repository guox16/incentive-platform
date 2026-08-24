package com.incentive.activity.controller;

import com.incentive.activity.application.AdminActivityService;
import com.incentive.activity.application.AdminPrizePoolService;
import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.dto.AdminActivityResponse;
import com.incentive.activity.dto.AdminPrizePoolResponse;
import com.incentive.activity.dto.CreateActivityRequest;
import com.incentive.activity.dto.UpdateActivityRequest;
import com.incentive.activity.dto.UpdatePrizePoolRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 活动管理接口；一期仅供受信管理端或接口调试工具调用。 */
@RestController
@RequestMapping("/api/v1/activities/admin")
@Tag(name = "活动管理")
public class AdminActivityController {
  private final AdminActivityService service;
  private final AdminPrizePoolService prizePoolService;

  public AdminActivityController(AdminActivityService service,
      AdminPrizePoolService prizePoolService) {
    this.service = service;
    this.prizePoolService = prizePoolService;
  }

  @GetMapping
  @Operation(summary = "查询活动列表")
  public List<AdminActivityResponse> list(
      @RequestParam(name = "type", required = false) ActivityType type,
      @RequestParam(name = "status", required = false) ActivityStatus status) {
    return service.list(type, status);
  }

  @GetMapping("/{id}")
  @Operation(summary = "查询活动管理详情")
  public AdminActivityResponse get(@PathVariable("id") Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "创建活动及首版参与规则")
  public AdminActivityResponse create(@Valid @RequestBody CreateActivityRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "修改活动并按需创建规则版本")
  public AdminActivityResponse update(@PathVariable("id") Long id,
      @Valid @RequestBody UpdateActivityRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "删除已结束且无参与记录的活动")
  public void delete(@PathVariable("id") Long id) {
    service.delete(id);
  }

  @GetMapping("/{id}/prize-pool")
  @Operation(summary = "查询奖池配置与可选奖品")
  public AdminPrizePoolResponse getPrizePool(@PathVariable("id") Long id,
      @org.springframework.web.bind.annotation.RequestHeader(HttpHeaders.AUTHORIZATION)
      String authorization) {
    return prizePoolService.get(id, authorization);
  }

  @PutMapping("/{id}/prize-pool")
  @Operation(summary = "保存草稿活动奖池")
  public AdminPrizePoolResponse updatePrizePool(@PathVariable("id") Long id,
      @Valid @RequestBody UpdatePrizePoolRequest request,
      @org.springframework.web.bind.annotation.RequestHeader(HttpHeaders.AUTHORIZATION)
      String authorization) {
    return prizePoolService.update(id, request, authorization);
  }
}
