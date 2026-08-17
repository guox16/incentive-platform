package com.incentive.user.security;

public record RotatedRefreshToken(Long userId, String value, long expiresInSeconds) {}
