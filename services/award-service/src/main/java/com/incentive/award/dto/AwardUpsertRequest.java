package com.incentive.award.dto;

import com.incentive.award.domain.AwardType;
import com.incentive.award.domain.AwardStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AwardUpsertRequest(
    @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull AwardType type,
    @NotNull AwardStatus status,
    @Size(max = 500) String coverUrl,
    String awardPayload,
    @PositiveOrZero long totalStock,
    @PositiveOrZero long availableStock) {

  @AssertTrue(message = "可用库存不能大于总库存")
  public boolean isStockValid() {
    return availableStock <= totalStock;
  }

  @AssertTrue(message = "谢谢参与奖品的库存必须为0")
  public boolean isNoneStockValid() {
    return type != AwardType.NONE || (totalStock == 0 && availableStock == 0);
  }

  @AssertTrue(message = "不能通过创建或更新接口直接设置软删除状态")
  public boolean isStatusValid() {
    return status != AwardStatus.DELETED;
  }
}
