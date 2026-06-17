package com.example.money_transfer_system.controller;

import com.example.money_transfer_system.dto.RewardResponse;
import com.example.money_transfer_system.dto.RewardSummaryResponse;
import com.example.money_transfer_system.security.JwtUtil;
import com.example.money_transfer_system.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<RewardResponse>> getRewards(
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        return ResponseEntity.ok(rewardService.getRewards(accountId));
    }

    @GetMapping("/total")
    public ResponseEntity<RewardSummaryResponse> getTotalPoints(
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        return ResponseEntity.ok(rewardService.getSummary(accountId));
    }

    @GetMapping("/group")
    public ResponseEntity<List<RewardResponse>> getGroupRewards(
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        return ResponseEntity.ok(rewardService.getGroupRewards(accountId));
    }

    @GetMapping("/group/total")
    public ResponseEntity<RewardSummaryResponse> getGroupTotalPoints(
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        return ResponseEntity.ok(rewardService.getGroupSummary(accountId));
    }

    private Long extractAccountId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractAccountId(token);
    }
}
