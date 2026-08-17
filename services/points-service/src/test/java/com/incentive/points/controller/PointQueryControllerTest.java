package com.incentive.points.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.config.PointsSecurityConfiguration;
import com.incentive.points.dto.PointTransactionPageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebMvcTest(PointQueryController.class)
@Import(PointsSecurityConfiguration.class)
class PointQueryControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private PointAccountService service;

  @Test
  void bindsPaginationParametersByTheirHttpNames() throws Exception {
    when(service.getTransactions(1L, 2, 30))
        .thenReturn(new PointTransactionPageResponse(List.of(), 2, 30, 0, 0));

    mockMvc.perform(get("/api/v1/points/me/transactions")
            .with(jwt().jwt(token -> token.subject("1"))
                .authorities(new SimpleGrantedAuthority("POINTS_SELF")))
            .header("X-User-Id", "999")
            .queryParam("page", "2")
            .queryParam("size", "30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(30));

    verify(service).getTransactions(1L, 2, 30);
  }

  @Test
  void rejectsQueryWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/points/me/balance").header("X-User-Id", "1"))
        .andExpect(status().isUnauthorized());
  }
}
