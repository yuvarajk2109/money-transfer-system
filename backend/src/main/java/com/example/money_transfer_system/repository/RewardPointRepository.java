package com.example.money_transfer_system.repository;

import com.example.money_transfer_system.entity.RewardPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardPointRepository extends JpaRepository<RewardPoint, Long> {

    List<RewardPoint> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<RewardPoint> findByAccountIdInOrderByCreatedAtDesc(List<Long> accountIds);

    List<RewardPoint> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardPoint r WHERE r.accountId = :accountId AND r.revoked = false")
    int getTotalPointsByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardPoint r WHERE r.accountId IN :accountIds AND r.revoked = false")
    int getTotalPointsByAccountIds(@Param("accountIds") List<Long> accountIds);
}
