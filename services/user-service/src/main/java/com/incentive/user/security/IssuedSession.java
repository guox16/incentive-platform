package com.incentive.user.security;

import com.incentive.user.dto.LoginResponse;

public record IssuedSession(
    LoginResponse response,
    String refreshToken,
    long refreshTokenExpiresInSeconds) {}
