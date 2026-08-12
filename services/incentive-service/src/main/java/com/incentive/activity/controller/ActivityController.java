package com.incentive.activity.controller;

import com.incentive.activity.application.ActivityQueryService;
import com.incentive.activity.dto.ActivityDetailResponse;
import com.incentive.activity.dto.ActivitySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "活动查询")
public class ActivityController {
  private final ActivityQueryService service;

  public ActivityController(ActivityQueryService service) {
    this.service = service;
  }

  @GetMapping("/active")
  @Operation(summary = "查询进行中的抽奖和兑换活动")
  public List<ActivitySummaryResponse> active() {
    return service.activeActivities();
  }

  @GetMapping("/{activityCode}")
  @Operation(summary = "查询活动详情")
  public ActivityDetailResponse detail(@PathVariable String activityCode) {
    return service.detail(activityCode);
  }
}
