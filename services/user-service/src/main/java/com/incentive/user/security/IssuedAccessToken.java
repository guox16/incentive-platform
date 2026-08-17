package com.incentive.user.security;

public record IssuedAccessToken(String value, long expiresInSeconds) {}
