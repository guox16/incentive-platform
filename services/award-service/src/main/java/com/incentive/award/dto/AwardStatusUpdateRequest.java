package com.incentive.award.dto;

import com.incentive.award.domain.AwardStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AwardStatusUpdateRequest(@NotNull AwardStatus status) {
  @AssertTrue(message = "不能通过状态更新接口设置软删除状态")
  public boolean isStatusValid() {
    return status != AwardStatus.DELETED;
  }
}
