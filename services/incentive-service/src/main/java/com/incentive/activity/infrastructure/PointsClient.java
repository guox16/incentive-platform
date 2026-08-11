package com.incentive.activity.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PointsClient {
  private final RestClient restClient;

  public PointsClient(RestClient.Builder builder, @Value("${clients.points.base-url}") String baseUrl) {
    this.restClient = builder.baseUrl(baseUrl).build();
  }

  public PointCreditResult credit(Long businessId, Long userId, long amount) {
    PointCreditResponse response = restClient.post()
        .uri("/api/v1/internal/points/credit")
        .body(new PointCreditRequest(businessId, userId, amount, "CHECK_IN", "每日签到奖励"))
        .retrieve()
        .body(PointCreditResponse.class);
    if (response == null) throw new IllegalStateException("积分服务未返回发放结果");
    return new PointCreditResult(response.transactionId(), response.balanceAfter());
  }

  private record PointCreditRequest(Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointCreditResponse(Long transactionId, long balanceAfter) {}
  public record PointCreditResult(Long transactionId, long balanceAfter) {}
}
