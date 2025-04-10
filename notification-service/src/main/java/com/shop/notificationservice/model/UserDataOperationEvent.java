package com.shop.notificationservice.model;

import java.time.LocalDateTime;

public record UserDataOperationEvent (
        String email,
        String activationCode,
        LocalDateTime expiresAt,
        LocalDateTime eventTime
) {}
