// src/main/java/com/example/money_transfer_system/service/TransferService.java
package com.example.money_transfer_system.service;

import java.time.LocalDateTime;
import com.example.money_transfer_system.config.AccountProperties;
import com.example.money_transfer_system.dto.TransferRequest;
import com.example.money_transfer_system.dto.TransferResponse;
import com.example.money_transfer_system.entity.Account;
import com.example.money_transfer_system.entity.TransactionLog;
import com.example.money_transfer_system.enums.AccountStatus;
import com.example.money_transfer_system.enums.TransactionStatus;
import com.example.money_transfer_system.enums.TransactionType;
import com.example.money_transfer_system.exception.*;
import com.example.money_transfer_system.repository.AccountRepository;
import com.example.money_transfer_system.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.money_transfer_system.config.RollbackProperties;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final AccountProperties accountProperties;

    private final RollbackProperties rollbackProperties;
    private final TransactionLogService transactionLogService;
    private final RewardService rewardService;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        try {
            // Idempotency
            if (transactionLogRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
                throw new DuplicateTransferException("Transfer with this idempotency key already exists (TRX-409)");
            }

            // Basic validations
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be greater than zero (VAL-422)");
            }

            if (request.getFromAccountId().equals(request.getToAccountId())) {
                throw new InvalidAmountException("Cannot transfer to the same account (VAL-422)");
            }

            // Load accounts
            Account fromAccount = accountRepository.findById(request.getFromAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found (ACC-404)"));

            Account toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found (ACC-404)"));

            // Status + approval
            if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new AccountNotActiveException("Source account is not active (ACC-403)");
            }
            if (toAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new AccountNotActiveException("Destination account is not active (ACC-403)");
            }
            if (!fromAccount.getApproved()) {
                throw new AccountNotActiveException("Source account is not approved (ACC-403)");
            }
            if (!toAccount.getApproved()) {
                throw new AccountNotActiveException("Destination account is not approved (ACC-403)");
            }

            int usedRewardPoints = request.getUsedRewardPoints() != null ? request.getUsedRewardPoints() : 0;

            if (usedRewardPoints > 0) {
                int activePoints = rewardService.getGroupSummary(fromAccount.getId()).getTotalPoints();
                if (usedRewardPoints > activePoints) {
                    throw new InvalidAmountException("Insufficient reward points (TRX-400)");
                }
                if (BigDecimal.valueOf(usedRewardPoints).compareTo(request.getAmount()) > 0) {
                    throw new InvalidAmountException("Cannot use more points than transfer amount (TRX-400)");
                }
            }

            BigDecimal actualDeduction = request.getAmount().subtract(BigDecimal.valueOf(usedRewardPoints));

            // Funds & minimum balance rules
            if (fromAccount.getBalance().compareTo(actualDeduction) < 0) {
                throw new InsufficientFundsException("Insufficient funds in source account (TRX-400)");
            }

            BigDecimal minimumBalance = accountProperties
                    .getMinimumBalance(fromAccount.getAccountType().name());

            BigDecimal balanceAfterTransfer = fromAccount.getBalance().subtract(actualDeduction);
            if (balanceAfterTransfer.compareTo(minimumBalance) < 0) {
                throw new InsufficientFundsException(
                        "Transfer would violate minimum balance requirement of " + minimumBalance + " (TRX-400)");
            }

            // Perform transfer
            fromAccount.setBalance(balanceAfterTransfer);
            toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Detect self-transfer (same email = same user, different account types)
            boolean isSelfTransfer = fromAccount.getEmail().equalsIgnoreCase(toAccount.getEmail());
            TransactionType txType = isSelfTransfer ? TransactionType.SELF_TRANSFER : TransactionType.DEBIT;
            TransactionType txTypeCredit = isSelfTransfer ? TransactionType.SELF_TRANSFER : TransactionType.CREDIT;

            // Success logs
            TransactionLog debitLog = new TransactionLog();
            debitLog.setFromAccountId(fromAccount.getId());
            debitLog.setToAccountId(toAccount.getId());
            debitLog.setAmount(request.getAmount());
            debitLog.setTransactionType(txType);
            debitLog.setStatus(TransactionStatus.SUCCESS);
            debitLog.setIdempotencyKey(request.getIdempotencyKey() + "-DEBIT");
            transactionLogRepository.save(debitLog);

            TransactionLog creditLog = new TransactionLog();
            creditLog.setFromAccountId(fromAccount.getId());
            creditLog.setToAccountId(toAccount.getId());
            creditLog.setAmount(request.getAmount());
            creditLog.setTransactionType(txTypeCredit);
            creditLog.setStatus(TransactionStatus.SUCCESS);
            creditLog.setIdempotencyKey(request.getIdempotencyKey() + "-CREDIT");
            transactionLogRepository.save(creditLog);

            log.info("{} of ₹{} from account ID {} to account ID {} successful",
                    isSelfTransfer ? "Self-transfer" : "Transfer",
                    request.getAmount(), fromAccount.getId(), toAccount.getId());

            // Process Rewards (skip for self-transfers)
            if (!isSelfTransfer) {
                try {
                    if (usedRewardPoints > 0) {
                        rewardService.usePoints(fromAccount.getId(), debitLog.getId(), usedRewardPoints);
                    }
                    rewardService.processReward(debitLog, usedRewardPoints);
                } catch (Exception rewardEx) {
                    log.error("Failed to process reward for transaction {}: {}", debitLog.getId(), rewardEx.getMessage());
                }
            }

            return new TransferResponse(
                    debitLog.getId(), // or creditLog.getId()
                    "SUCCESS",
                    "Transfer completed successfully",
                    fromAccount.getId(),
                    toAccount.getId(),
                    request.getAmount());

        } catch (RuntimeException ex) {
            // NEW: Persist a FAILURE log in its own transaction
            try {
                // Use TRANSFER as the logical operation type.
                // If your enum lacks TRANSFER, change to TransactionType.DEBIT or add TRANSFER
                // to the enum.
                transactionLogService.logFailure(
                        request.getFromAccountId(),
                        request.getToAccountId(),
                        request.getAmount(),
                        TransactionType.TRANSFER,
                        request.getIdempotencyKey(), // the service appends "-FAIL" safely
                        ex.getMessage());
            } catch (Exception loggingEx) {
                // Never swallow the original exception; just report logging issues.
                log.error("Failed to persist failure log: {}", loggingEx.getMessage(), loggingEx);
            }
            throw ex; // keep existing behavior (transaction rollback & error propagation)
        }
    }

    public List<TransactionLog> getTransactionHistory(Long accountId) {
        return transactionLogRepository.findByAccountId(accountId);
    }

    public List<TransactionLog> getAllTransactions() {
        return transactionLogRepository.findAllByOrderByCreatedOnDesc();
    }

    @Transactional
    public void requestRollback(String transactionId, Long requesterId) {

        TransactionLog original = transactionLogRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!original.getFromAccountId().equals(requesterId)) {
            throw new RuntimeException("You can only request rollback for your own transaction");
        }

        if (original.getStatus() != TransactionStatus.SUCCESS) {
            throw new RuntimeException("Only successful transactions can be rolled back");
        }

        LocalDateTime expiryTime = original.getCreatedOn()
                .plusMinutes(rollbackProperties.getWindowMinutes());

        if (LocalDateTime.now().isAfter(expiryTime)) {
            throw new RuntimeException("Rollback window expired");
        }

        original.setStatus(TransactionStatus.ROLLBACK_REQUESTED);
        original.setRollbackRequestedAt(LocalDateTime.now());

        transactionLogRepository.save(original);

        log.info("Rollback requested for transaction {}", transactionId);
    }

    @Transactional
    public void approveRollback(String transactionId, Long adminId) {

        TransactionLog original = transactionLogRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (original.getStatus() != TransactionStatus.ROLLBACK_REQUESTED) {
            throw new RuntimeException("Rollback not requested");
        }

        if (transactionLogRepository.existsByOriginalTransactionId(transactionId)) {
            throw new RuntimeException("Transaction already reversed");
        }

        Account fromAccount = accountRepository.findById(original.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account toAccount = accountRepository.findById(original.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Destination account not found"));

        BigDecimal amount = original.getAmount();
        int usedPoints = rewardService.getUsedPointsForTransaction(transactionId);
        BigDecimal actualRefund = amount.subtract(BigDecimal.valueOf(usedPoints));

        // Ensure destination has sufficient funds
        if (toAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Destination account has insufficient funds for rollback");
        }

        // Respect minimum balance
        BigDecimal minBalance = accountProperties
                .getMinimumBalance(toAccount.getAccountType().name());

        BigDecimal afterDebit = toAccount.getBalance().subtract(amount);

        if (afterDebit.compareTo(minBalance) < 0) {
            throw new RuntimeException("Rollback violates minimum balance requirement");
        }

        // Reverse balances
        toAccount.setBalance(afterDebit);
        fromAccount.setBalance(fromAccount.getBalance().add(actualRefund));

        accountRepository.save(toAccount);
        accountRepository.save(fromAccount);

        // Update original transaction
        original.setStatus(TransactionStatus.ROLLED_BACK);
        original.setRollbackProcessedAt(LocalDateTime.now());
        original.setRollbackProcessedBy(adminId);

        transactionLogRepository.save(original);

        // Create REVERSAL log entry
        TransactionLog reversal = new TransactionLog();
        reversal.setFromAccountId(toAccount.getId());
        reversal.setToAccountId(fromAccount.getId());
        reversal.setAmount(amount);
        reversal.setTransactionType(TransactionType.REVERSAL);
        reversal.setStatus(TransactionStatus.SUCCESS);
        reversal.setOriginalTransactionId(original.getId());
        reversal.setIdempotencyKey(original.getId() + "-REVERSAL");

        transactionLogRepository.save(reversal);

        log.info("Admin {} approved rollback for transaction ID: {}", adminId, transactionId);

        // Revoke Rewards
        try {
            rewardService.revokeReward(transactionId);
        } catch (Exception rewardEx) {
            log.error("Failed to revoke reward for transaction {}: {}", transactionId, rewardEx.getMessage());
        }
    }

    @Transactional
    public void rejectRollback(String transactionId) {

        TransactionLog original = transactionLogRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (original.getStatus() != TransactionStatus.ROLLBACK_REQUESTED) {
            throw new RuntimeException("Rollback not requested");
        }

        original.setStatus(TransactionStatus.ROLLBACK_REJECTED);
        original.setRollbackProcessedAt(LocalDateTime.now());

        transactionLogRepository.save(original);

        log.info("Admin rejected rollback for transaction ID: {}", transactionId);
    }

    public List<TransactionLog> getPendingRollbacks() {
        return transactionLogRepository.findByStatus(TransactionStatus.ROLLBACK_REQUESTED);
    }

}