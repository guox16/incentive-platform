package com.incentive.award.dto;

import com.incentive.award.domain.AwardStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record BatchAwardStatusUpdateRequest(
    @NotEmpty List<@NotNull @Positive Long> ids,
    @NotNull AwardStatus status) {
  @AssertTrue(message = "不能通过状态更新接口设置软删除状态")
  public boolean isStatusValid() {
    return status != AwardStatus.DELETED;
  }
}
