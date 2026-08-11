package com.incentive.activity.infrastructure;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class BusinessNumberGenerator {
  public long next() {
    return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
  }
}
