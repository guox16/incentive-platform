package com.incentive.activity.infrastructure;

import com.incentive.common.api.ApiError;
import com.incentive.activity.support.IncentiveBusinessException;
import com.incentive.activity.security.InternalJwtTokenService;
import java.time.Instant;
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
    try {
      PointCreditResponse response = restClient.post()
          .uri("/api/v1/internal/points/credit")
          .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
          .body(new PointCreditRequest(businessId, userId, amount, "CHECK_IN", "每日签到奖励"))
          .retrieve()
          .body(PointCreditResponse.class);
      if (response == null) throw new RestClientException("积分服务未返回发放结果");
      return new PointCreditResult(response.transactionId(), response.balanceAfter());
    } catch (RestClientException ex) {
      throw new IncentiveBusinessException(
          "CHECK_IN_REWARD_PENDING", "签到已记录，积分发放暂未完成，请重试",
          HttpStatus.SERVICE_UNAVAILABLE);
    }
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

  /** 预占积分；相同业务号重试时由积分服务返回原预占结果。 */
  public PointReservationResult reserve(
      Long businessId, Long userId, long amount, String source, String remark) {
    try {
      PointReservationResponse response = restClient.post()
          .uri("/api/v1/internal/points/reservations")
          .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
          .body(new PointReservationRequest(businessId, userId, amount, source, remark))
          .retrieve()
          .body(PointReservationResponse.class);
      return requireReservationResponse(response, "积分服务未返回预占结果");
    } catch (RestClientResponseException ex) {
      throw translateReservationError(ex);
    } catch (RestClientException ex) {
      throw pointsUnavailable();
    }
  }

  /** 确认积分预占并取得正式扣减流水。 */
  public PointReservationResult confirmReservation(Long businessId) {
    try {
      PointReservationResponse response = restClient.post()
          .uri("/api/v1/internal/points/reservations/{businessId}/confirm", businessId)
          .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
          .retrieve()
          .body(PointReservationResponse.class);
      PointReservationResult result = requireReservationResponse(
          response, "积分服务未返回预占确认结果");
      if (result.confirmedTransactionId() == null) {
        throw invalidResponse("积分预占确认后未返回扣减流水ID");
      }
      return result;
    } catch (RestClientResponseException ex) {
      throw translateReservationError(ex);
    } catch (RestClientException ex) {
      throw pointsUnavailable();
    }
  }

  /** 查询既有积分预占，供异常抽奖流程对账使用。 */
  public PointReservationResult getReservation(Long businessId) {
    try {
      PointReservationResponse response = restClient.get()
          .uri("/api/v1/internal/points/reservations/{businessId}", businessId)
          .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
          .retrieve()
          .body(PointReservationResponse.class);
      return requireReservationResponse(response, "积分服务未返回预占查询结果");
    } catch (RestClientResponseException ex) {
      throw translateReservationError(ex);
    } catch (RestClientException ex) {
      throw pointsUnavailable();
    }
  }

  /** 取消尚未确认的积分预占并退回积分。 */
  public PointReservationResult cancelReservation(Long businessId) {
    try {
      PointReservationResponse response = restClient.post()
          .uri("/api/v1/internal/points/reservations/{businessId}/cancel", businessId)
          .headers(headers -> headers.setBearerAuth(tokenService.issuePointsCommandToken()))
          .retrieve()
          .body(PointReservationResponse.class);
      return requireReservationResponse(response, "积分服务未返回预占取消结果");
    } catch (RestClientResponseException ex) {
      throw translateReservationError(ex);
    } catch (RestClientException ex) {
      throw pointsUnavailable();
    }
  }

  private PointReservationResult requireReservationResponse(
      PointReservationResponse response, String message) {
    if (response == null) throw invalidResponse(message);
    return toReservationResult(response);
  }

  private IncentiveBusinessException translateReservationError(RestClientResponseException ex) {
    if (ex.getStatusCode().is5xxServerError()) {
      return new IncentiveBusinessException(
          "POINTS_SERVICE_ERROR", "积分服务处理失败", HttpStatus.BAD_GATEWAY);
    }
    try {
      ApiError error = ex.getResponseBodyAs(ApiError.class);
      if (error != null && error.code() != null && !error.code().isBlank()) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return new IncentiveBusinessException(error.code(), error.message(),
            status == null ? HttpStatus.BAD_GATEWAY : status);
      }
    } catch (RuntimeException ignored) {
      // 无法解析下游错误体时使用稳定的兜底错误码。
    }
    return new IncentiveBusinessException(
        "POINTS_RESERVATION_REJECTED", "积分预占请求被拒绝", HttpStatus.CONFLICT);
  }

  private IncentiveBusinessException pointsUnavailable() {
    return new IncentiveBusinessException(
        "POINTS_SERVICE_UNAVAILABLE", "积分服务暂不可用", HttpStatus.BAD_GATEWAY);
  }

  private IncentiveBusinessException invalidResponse(String message) {
    return new IncentiveBusinessException(
        "POINTS_SERVICE_INVALID_RESPONSE", message, HttpStatus.BAD_GATEWAY);
  }

  private PointReservationResult toReservationResult(PointReservationResponse response) {
    return new PointReservationResult(
        response.businessId(), response.balanceAfter(), response.status(),
        response.confirmedTransactionId(), response.expiresAt(), response.replayed());
  }

  private record PointCreditRequest(Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointCreditResponse(Long transactionId, long balanceAfter) {}
  private record PointDebitRequest(Long businessId, Long userId, long amount, String source, String remark) {}
  private record PointDebitResponse(Long transactionId, long balanceAfter) {}
  private record PointReservationRequest(Long businessId, Long userId, long amount,
                                         String source, String remark) {}
  private record PointReservationResponse(Long businessId, long balanceAfter, String status,
                                          Long confirmedTransactionId, Instant expiresAt,
                                          boolean replayed) {}
  public record PointCreditResult(Long transactionId, long balanceAfter) {}
  public record PointDebitResult(Long transactionId, long balanceAfter) {}
  public record PointReservationResult(Long businessId, long balanceAfter, String status,
                                       Long confirmedTransactionId, Instant expiresAt,
                                       boolean replayed) {}
}
