package com.example.money_transfer_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardResponse {
    private Long id;
    private Long accountId;
    private String transactionId;
    private Integer points;
    private Boolean revoked;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    private BigDecimal transactionAmount;
}
