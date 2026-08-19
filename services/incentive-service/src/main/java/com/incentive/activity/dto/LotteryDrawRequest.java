package com.incentive.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LotteryDrawRequest(
    @NotBlank(message = "requestId不能为空")
    @Size(max = 64, message = "requestId长度不能超过64个字符")
    String requestId) {}
