package com.example.money_transfer_system.repository;

import com.example.money_transfer_system.entity.Account;
import com.example.money_transfer_system.enums.AccountStatus;
import com.example.money_transfer_system.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByEmail(String email);
    Optional<Account> findByEmailAndAccountType(String email, AccountType accountType);
    Optional<Account> findFirstByEmailOrderByCreatedAtAsc(String email);

    List<Account> findAllByEmail(String email);
    
    boolean existsByEmail(String email);

    boolean existsByEmailAndAccountType(String email, AccountType accountType);
    
    List<Account> findByApprovedFalseAndStatus(AccountStatus status);
    
    List<Account> findByStatus(AccountStatus status);

    List<Account> findByHolderNameAndEmail(String holderName, String email);
}
