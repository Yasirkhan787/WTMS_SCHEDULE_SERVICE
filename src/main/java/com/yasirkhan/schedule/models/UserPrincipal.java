package com.yasirkhan.schedule.models;

public record UserPrincipal(
        String userId,
        String username,
        String role
) {}