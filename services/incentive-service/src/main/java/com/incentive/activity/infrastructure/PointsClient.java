package com.incentive.activity.infrastructure;

import com.incentive.activity.support.IncentiveBusinessException;
import com.incentive.activity.security.InternalJwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PointsClient {
  private final RestClient restClient;
  private final InternalJwtTokenService tokenService;

  public PointsClient(RestClient.Builder builder, @Value("${clients.points.base-url}") String baseUrl,
      InternalJwtTokenService tokenService) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.tokenService = tokenService;
  }

  public PointCreditResult credit(Long businessId, Long userId, long amount) {
    PointCreditResponse response = restClient.post()
        .uri("/api/v1/internal/points/credit")
        .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
        .body(new PointCreditRequest(businessId, userId, amount, "CHECK_IN", "每日签到奖励"))
        .retrieve()
        .body(PointCreditResponse.class);
    if (response == null) throw new IllegalStateException("积分服务未返回发放结果");
    return new PointCreditResult(response.transactionId(), response.balanceAfter());
  }

  public PointDebitResult debit(Long businessId, Long userId, long amount, String source, String remark) {
    try {
      PointDebitResponse response = restClient.post()
          .uri("/api/v1/internal/points/debit")
          .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
          .body(new PointDebitRequest(businessId, userId, amount, source, remark))
          .retrieve()
          .body(PointDebitResponse.class);
      if (response == null) throw new IllegalStateException("积分服务未返回扣减结果");
      return new PointDebitResult(response.transactionId(), response.balanceAfter());
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 409) {
        throw new IncentiveBusinessException(
            "INSUFFICIENT_POINTS", "用户积分不足", HttpStatus.CONFLICT);
      }
      throw new IncentiveBusinessException(
          "POINTS_SERVICE_ERROR", "积分扣减失败", HttpStatus.BAD_GATEWAY);
    } catch (RestClientException ex) {
      throw new IncentiveBusinessException(
          "POINTS_SERVICE_UNAVAILABLE", "积分服务暂不可用", HttpStatus.BAD_GATEWAY);
    }
  }

  private record PointCreditRequest(Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointCreditResponse(Long transactionId, long balanceAfter) {}
  private record PointDebitRequest(Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointDebitResponse(Long transactionId, long balanceAfter) {}
  public record PointCreditResult(Long transactionId, long balanceAfter) {}
  public record PointDebitResult(Long transactionId, long balanceAfter) {}
}
