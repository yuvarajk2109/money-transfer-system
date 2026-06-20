package com.example.money_transfer_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSummaryResponse {
    private int totalPoints;
    private long totalRewards;
    private int revokedPoints;
    private int usedPoints;
    private int totalPointsLifetime;
}
