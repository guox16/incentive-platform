package com.incentive.activity.infrastructure;

import com.incentive.activity.application.AwardCatalog;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAwardCatalog implements AwardCatalog {
  private final RestClient restClient;

  public HttpAwardCatalog(RestClient.Builder builder,
      @Value("${clients.award.base-url}") String baseUrl) {
    this.restClient = builder.baseUrl(baseUrl).build();
  }

  @Override
  public List<Item> list(String authorization) {
    try {
      List<Item> result = restClient.get()
          .uri("/api/v1/awards")
          .header(HttpHeaders.AUTHORIZATION, authorization)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
      return result == null ? List.of() : result;
    } catch (RestClientException ex) {
      throw new IncentiveBusinessException(
          "AWARD_CATALOG_UNAVAILABLE", "奖品目录暂不可用，请稍后重试", HttpStatus.BAD_GATEWAY);
    }
  }
}
