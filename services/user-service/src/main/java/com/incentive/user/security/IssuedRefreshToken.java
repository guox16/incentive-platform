package com.incentive.user.security;

public record IssuedRefreshToken(String value, long expiresInSeconds) {}
