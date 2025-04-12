package com.shop.notificationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDataOperationEvent {
        String email;
        String activationCode;
        LocalDateTime expiresAt;
        LocalDateTime eventTime;
}
