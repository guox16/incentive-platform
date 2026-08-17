package com.incentive.common.security;

public final class AccessTokenBlacklistKeys {
  public static final String JTI_PREFIX = "auth:access:blacklist:jti:";
  public static final String USER_CUTOFF_PREFIX = "auth:access:blacklist:user:";

  private AccessTokenBlacklistKeys() {}

  public static String jti(String tokenId) {
    return JTI_PREFIX + tokenId;
  }

  public static String userCutoff(String userId) {
    return USER_CUTOFF_PREFIX + userId;
  }
}
