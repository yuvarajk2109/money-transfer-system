package com.example.money_transfer_system.service;

import com.example.money_transfer_system.dto.RewardResponse;
import com.example.money_transfer_system.dto.RewardSummaryResponse;
import com.example.money_transfer_system.entity.RewardPoint;
import com.example.money_transfer_system.entity.TransactionLog;
import com.example.money_transfer_system.enums.TransactionStatus;
import com.example.money_transfer_system.repository.RewardPointRepository;
import com.example.money_transfer_system.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final RewardPointRepository rewardPointRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final LinkedAccountService linkedAccountService;

    /**
     * Process reward for a successful transfer transaction (DEBIT log).
     * Eligibility: status=SUCCESS, amount>100, sender!=receiver, not self-transfer
     * via linked accounts.
     */
    @Transactional
    public void processReward(TransactionLog transaction, int usedPoints) {

        // Must be SUCCESS
        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            return;
        }

        // Amount must be >= 100
        if (transaction.getAmount().compareTo(new BigDecimal("100")) < 0) {
            return;
        }

        // Sender and receiver must be different
        if (transaction.getFromAccountId().equals(transaction.getToAccountId())) {
            return;
        }

        // Check if sender and receiver are linked (self-transfer)
        if (linkedAccountService.areLinked(transaction.getFromAccountId(), transaction.getToAccountId())) {
            log.info("Skipping reward for self-transfer between linked accounts: {} -> {}",
                    transaction.getFromAccountId(), transaction.getToAccountId());
            return;
        }

        BigDecimal actualSpent = transaction.getAmount().subtract(new BigDecimal(usedPoints));
        if (actualSpent.compareTo(new BigDecimal("100")) < 0) {
            return;
        }

        // Calculate points: floor(actualSpent / 100)
        int points = actualSpent.divideToIntegralValue(new BigDecimal("100")).intValue();

        if (points <= 0) {
            return;
        }

        RewardPoint reward = new RewardPoint();
        reward.setAccountId(transaction.getFromAccountId());
        reward.setTransactionId(transaction.getId());
        reward.setPoints(points);
        reward.setRevoked(false);

        rewardPointRepository.save(reward);
        log.info("Awarded {} reward points for transaction {} to account {}",
                points, transaction.getId(), transaction.getFromAccountId());
    }

    @Transactional
    public void usePoints(Long accountId, String transactionId, int pointsUsed) {
        if (pointsUsed <= 0)
            return;
        RewardPoint reward = new RewardPoint();
        reward.setAccountId(accountId);
        reward.setTransactionId(transactionId);
        reward.setPoints(-pointsUsed);
        reward.setRevoked(false);
        rewardPointRepository.save(reward);
        log.info("Used {} reward points for transaction {} by account {}",
                pointsUsed, transactionId, accountId);
    }

    /**
     * Revoke reward points when a transaction is rolled back.
     */
    @Transactional
    public void revokeReward(String transactionId) {
        List<RewardPoint> rewards = rewardPointRepository.findByTransactionId(transactionId);
        for (RewardPoint reward : rewards) {
            if (!reward.getRevoked()) {
                reward.setRevoked(true);
                reward.setRevokedAt(LocalDateTime.now());
                rewardPointRepository.save(reward);
                log.info("Revoked {} reward points for transaction {}", reward.getPoints(), transactionId);
            }
        }
    }

    public int getUsedPointsForTransaction(String transactionId) {
        return rewardPointRepository.findByTransactionId(transactionId).stream()
                .filter(r -> r.getPoints() < 0)
                .mapToInt(r -> Math.abs(r.getPoints()))
                .sum();
    }

    public List<RewardResponse> getRewards(Long accountId) {
        return rewardPointRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RewardSummaryResponse getSummary(Long accountId) {
        List<RewardPoint> rewards = rewardPointRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        return calculateSummary(rewards);
    }

    public List<RewardResponse> getGroupRewards(Long accountId) {
        List<Long> linkedIds = linkedAccountService.getLinkedAccountIds(accountId);
        return rewardPointRepository.findByAccountIdInOrderByCreatedAtDesc(linkedIds)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RewardSummaryResponse getGroupSummary(Long accountId) {
        List<Long> linkedIds = linkedAccountService.getLinkedAccountIds(accountId);
        List<RewardPoint> rewards = rewardPointRepository.findByAccountIdInOrderByCreatedAtDesc(linkedIds);
        return calculateSummary(rewards);
    }

    private RewardSummaryResponse calculateSummary(List<RewardPoint> rewards) {
        int totalActive = rewards.stream().filter(r -> !r.getRevoked()).mapToInt(RewardPoint::getPoints).sum();
        int revoked = rewards.stream().filter(r -> r.getRevoked() && r.getPoints() > 0).mapToInt(RewardPoint::getPoints)
                .sum();
        int usedPoints = rewards.stream().filter(r -> !r.getRevoked() && r.getPoints() < 0)
                .mapToInt(r -> Math.abs(r.getPoints())).sum();
        int totalPointsLifetime = rewards.stream().filter(r -> !r.getRevoked() && r.getPoints() > 0)
                .mapToInt(RewardPoint::getPoints).sum();
        int totalRewardsCount = (int) rewards.stream().filter(r -> !r.getRevoked()).count();
        return new RewardSummaryResponse(totalActive, totalRewardsCount, revoked, usedPoints, totalPointsLifetime);
    }

    private RewardResponse toResponse(RewardPoint rp) {
        BigDecimal txAmount = BigDecimal.ZERO;
        Optional<TransactionLog> txOpt = transactionLogRepository.findById(rp.getTransactionId());
        if (txOpt.isPresent()) {
            txAmount = txOpt.get().getAmount();
        }
        return new RewardResponse(
                rp.getId(), rp.getAccountId(), rp.getTransactionId(),
                rp.getPoints(), rp.getRevoked(), rp.getCreatedAt(),
                rp.getRevokedAt(), txAmount);
    }
}
