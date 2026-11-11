package com.incentive.award.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incentive.award.config.AwardSecurityConfiguration;
import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardSourceType;
import com.incentive.award.domain.AwardType;
import com.incentive.award.domain.UserAward;
import com.incentive.award.messaging.AwardCommandMessage;
import com.incentive.award.repository.UserAwardRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAwardController.class)
@Import(AwardSecurityConfiguration.class)
class UserAwardControllerTest {
  @Autowired private MockMvc mvc;
  @MockBean private UserAwardRepository repository;
  @MockBean private StringRedisTemplate redis;

  @Test
  void queriesOnlyTheAuthenticatedUsersAwards() throws Exception {
    Instant obtained = Instant.parse("2026-11-11T00:00:00Z");
    UserAward award = new UserAward(new AwardIssuance(new AwardCommandMessage(null,
        "SHOP_REDEMPTION:34", AwardSourceType.SHOP_REDEMPTION, 34L, 7L, 12L,
        "咖啡券", AwardType.VIRTUAL, "{\"templateCode\":\"COFFEE\"}", null), null, obtained), obtained);
    ReflectionTestUtils.setField(award, "id", 45L);
    PageRequest page = PageRequest.of(0, 10);
    when(repository.findByUserIdOrderByObtainedAtDescIdDesc(7L, page))
        .thenReturn(new PageImpl<>(List.of(award), page, 1));

    mvc.perform(get("/api/v1/awards/me").param("userId", "99")
            .with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("ACCOUNT_SELF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("45"))
        .andExpect(jsonPath("$.items[0].name").value("咖啡券"))
        .andExpect(jsonPath("$.items[0].source").value("REDEMPTION"))
        .andExpect(jsonPath("$.items[0].obtainedAt").value("2026-11-11T00:00:00Z"))
        .andExpect(jsonPath("$.items[0].awardPayload").doesNotExist());
    verify(repository).findByUserIdOrderByObtainedAtDescIdDesc(7L, page);
  }

  @Test
  void requiresAccountSelfPermission() throws Exception {
    mvc.perform(get("/api/v1/awards/me").with(jwt().jwt(token -> token.subject("7"))
            .authorities(new SimpleGrantedAuthority("PRIZE_MANAGE"))))
        .andExpect(status().isForbidden());
    verifyNoInteractions(repository);
  }
}
