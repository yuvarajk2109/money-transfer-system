package com.example.money_transfer_system.repository;

import com.example.money_transfer_system.entity.LinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    Optional<LinkedAccount> findByAccountId(Long accountId);

    List<LinkedAccount> findByGroupId(String groupId);

    boolean existsByAccountId(Long accountId);
}
