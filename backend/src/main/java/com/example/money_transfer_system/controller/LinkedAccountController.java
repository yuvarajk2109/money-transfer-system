package com.example.money_transfer_system.controller;

import com.example.money_transfer_system.entity.Account;
import com.example.money_transfer_system.security.JwtUtil;
import com.example.money_transfer_system.service.LinkedAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class LinkedAccountController {

    private final LinkedAccountService linkedAccountService;
    private final JwtUtil jwtUtil;

    @GetMapping("/linked")
    public ResponseEntity<List<Account>> getLinkedAccounts(
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        return ResponseEntity.ok(linkedAccountService.getLinkedAccounts(accountId));
    }

    @GetMapping("/linkable")
    public ResponseEntity<List<Account>> getLinkableAccounts(
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        return ResponseEntity.ok(linkedAccountService.getLinkableAccounts(accountId));
    }

    @PostMapping("/link/{targetId}")
    public ResponseEntity<Map<String, Object>> linkAccount(
            @PathVariable Long targetId,
            @RequestHeader("Authorization") String authHeader) {
        Long accountId = extractAccountId(authHeader);
        linkedAccountService.linkAccount(accountId, targetId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Accounts linked successfully");
        return ResponseEntity.ok(response);
    }

    private Long extractAccountId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractAccountId(token);
    }
}
