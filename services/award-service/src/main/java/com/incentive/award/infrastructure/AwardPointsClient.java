package com.incentive.award.infrastructure;

import com.incentive.award.security.AwardPointsTokenService;
import com.incentive.common.api.ApiError;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AwardPointsClient {
  private final RestClient restClient;
  private final AwardPointsTokenService tokenService;

  public AwardPointsClient(RestClient.Builder builder,
      @Value("${clients.points.base-url}") String baseUrl,
      AwardPointsTokenService tokenService) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(3000);
    requestFactory.setReadTimeout(5000);
    restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    this.tokenService = tokenService;
  }

  public Optional<PointDebitResult> debit(Long businessId, Long userId, long amount, String awardName) {
    try {
      PointDebitResponse response = restClient.post()
          .uri("/api/v1/internal/points/debit")
          .headers(headers -> headers.setBearerAuth(tokenService.issue()))
          .body(new PointDebitRequest(
              businessId, userId, amount, "REDEMPTION", "兑换商品：" + awardName))
          .retrieve()
          .body(PointDebitResponse.class);
      if (response == null || response.transactionId() == null || response.balanceAfter() == null) {
        throw new RestClientException("积分服务未返回兑换流水");
      }
      return Optional.of(new PointDebitResult(response.transactionId(), response.balanceAfter()));
    } catch (RestClientResponseException ex) {
      if (insufficientPoints(ex)) return Optional.empty();
      throw new AwardDeliveryException("POINTS_REDEMPTION_FAILED", "兑换扣积分暂未完成", ex);
    } catch (RestClientException ex) {
      throw new AwardDeliveryException("POINTS_REDEMPTION_FAILED", "兑换扣积分暂未完成", ex);
    }
  }

  private boolean insufficientPoints(RestClientResponseException failure) {
    if (failure.getStatusCode().value() != 409) return false;
    try {
      ApiError error = failure.getResponseBodyAs(ApiError.class);
      return error != null && "INSUFFICIENT_POINTS".equals(error.code());
    } catch (RuntimeException invalidBody) {
      return false;
    }
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
  private record PointDebitRequest(
      Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointDebitResponse(Long transactionId, Long balanceAfter) {}
  public record PointDebitResult(Long transactionId, long balanceAfter) {}
}
