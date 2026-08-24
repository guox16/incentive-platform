package com.incentive.award.infrastructure;

import com.incentive.award.security.AwardPointsTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AwardPointsClient {
  private final RestClient restClient;
  private final AwardPointsTokenService tokenService;

  public AwardPointsClient(RestClient.Builder builder,
      @Value("${clients.points.base-url}") String baseUrl,
      AwardPointsTokenService tokenService) {
    restClient = builder.baseUrl(baseUrl).build();
    this.tokenService = tokenService;
  }

  public PointCreditResult credit(Long businessId, Long userId, long amount, String awardName) {
    try {
      PointCreditResponse response = restClient.post()
          .uri("/api/v1/internal/points/credit")
          .headers(headers -> headers.setBearerAuth(tokenService.issue()))
          .body(new PointCreditRequest(
              businessId, userId, amount, "AWARD", "发放奖品：" + awardName))
          .retrieve()
          .body(PointCreditResponse.class);
      if (response == null || response.transactionId() == null) {
        throw new RestClientException("积分服务未返回发奖流水");
      }
      return new PointCreditResult(response.transactionId(), response.balanceAfter());
    } catch (RestClientException ex) {
      throw new AwardDeliveryException("POINTS_AWARD_FAILED", "积分奖励发放失败", ex);
    }
  }

  private record PointCreditRequest(
      Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointCreditResponse(Long transactionId, long balanceAfter) {}
  public record PointCreditResult(Long transactionId, long balanceAfter) {}
}
