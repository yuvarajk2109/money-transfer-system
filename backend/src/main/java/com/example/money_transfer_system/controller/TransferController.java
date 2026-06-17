package com.example.money_transfer_system.controller;

import com.example.money_transfer_system.dto.TransferRequest;
import com.example.money_transfer_system.dto.TransferResponse;
import com.example.money_transfer_system.entity.TransactionLog;
import com.example.money_transfer_system.security.JwtUtil;
import com.example.money_transfer_system.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final JwtUtil jwtUtil;
    private final com.example.money_transfer_system.service.LinkedAccountService linkedAccountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extract account ID from JWT
        String token = authHeader.substring(7);
        Long authenticatedAccountId = jwtUtil.extractAccountId(token);
        String role = jwtUtil.extractRole(token);

        // Non-admin users can transfer from their own or linked accounts
        if (!role.equals("ROLE_ADMIN")) {
            java.util.List<Long> linkedIds = linkedAccountService.getLinkedAccountIds(authenticatedAccountId);
            if (!linkedIds.contains(request.getFromAccountId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only transfer from your own account");
            }
        }

        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<TransactionLog>> getTransactionHistory(
            @RequestParam(required = false) Long accountId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        Long authenticatedAccountId = jwtUtil.extractAccountId(token);

        Long targetAccountId = accountId != null ? accountId : authenticatedAccountId;

        // Verify the user owns the target account (or is admin)
        String role = jwtUtil.extractRole(token);
        if (!role.equals("ROLE_ADMIN") && !targetAccountId.equals(authenticatedAccountId)) {
            java.util.List<Long> linkedIds = linkedAccountService.getLinkedAccountIds(authenticatedAccountId);
            if (!linkedIds.contains(targetAccountId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only view your own transaction history");
            }
        }

        List<TransactionLog> history = transferService.getTransactionHistory(targetAccountId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{transactionId}/rollback")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> requestRollback(
            @PathVariable String transactionId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long accountId = jwtUtil.extractAccountId(token);

        transferService.requestRollback(transactionId, accountId);

        return ResponseEntity.ok("Rollback request submitted successfully");
    }

}
