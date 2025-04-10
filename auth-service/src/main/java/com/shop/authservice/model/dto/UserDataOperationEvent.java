package com.shop.authservice.model.dto;

import java.time.LocalDateTime;

public record UserDataOperationEvent(
        String email,
        String activationCode,
        LocalDateTime expiresAt,
        LocalDateTime eventTime
) {}
