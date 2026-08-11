package com.incentive.points.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.dto.PointTransactionPageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointQueryController.class)
class PointQueryControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private PointAccountService service;

  @Test
  void bindsPaginationParametersByTheirHttpNames() throws Exception {
    when(service.getTransactions(1L, 2, 30))
        .thenReturn(new PointTransactionPageResponse(List.of(), 2, 30, 0, 0));

    mockMvc.perform(get("/api/v1/points/users/1/transactions")
            .queryParam("page", "2")
            .queryParam("size", "30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(30));

    verify(service).getTransactions(1L, 2, 30);
  }
}
