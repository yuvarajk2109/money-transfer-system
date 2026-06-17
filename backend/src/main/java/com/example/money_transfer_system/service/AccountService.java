package com.example.money_transfer_system.service;

import com.example.money_transfer_system.config.AccountProperties;
import com.example.money_transfer_system.dto.DepositRequest;

import com.example.money_transfer_system.dto.AccountSearch;
import com.example.money_transfer_system.dto.RegisterRequest;
import com.example.money_transfer_system.entity.Account;
import com.example.money_transfer_system.entity.TransactionLog;
import com.example.money_transfer_system.enums.AccountStatus;
import com.example.money_transfer_system.enums.Role;
import com.example.money_transfer_system.enums.TransactionStatus;
import com.example.money_transfer_system.enums.TransactionType;
import com.example.money_transfer_system.exception.AccountNotFoundException;
import com.example.money_transfer_system.exception.InvalidAmountException;
import com.example.money_transfer_system.repository.AccountRepository;
import com.example.money_transfer_system.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final LinkedAccountService linkedAccountService;

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final PasswordEncoder passwordEncoder;

    private final AccountProperties accountProperties;

    @org.springframework.beans.factory.annotation.Value("${admin.default.email:admin@system.com}")
    private String adminEmail;

    @Transactional
    public Account registerAccount(RegisterRequest registerRequest) {

        if (adminEmail.equalsIgnoreCase(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Cannot register with admin email");
        }

        if (accountRepository.existsByEmailAndAccountType(registerRequest.getEmail(),
                registerRequest.getAccountType())) {
            throw new IllegalArgumentException("Email already registered with this account type");
        }

        // Password-match check for linked accounts
        List<Account> existingAccounts = accountRepository.findAllByEmail(registerRequest.getEmail());
        if (!existingAccounts.isEmpty()) {
            Account existing = existingAccounts.get(0);

            if (!passwordEncoder.matches(registerRequest.getPassword(), existing.getPasswordHash())) {
                throw new IllegalArgumentException("Password must match your existing account");
            }
            if (!existing.getHolderName().equalsIgnoreCase(registerRequest.getHolderName())) {
                throw new IllegalArgumentException("Name must match your existing account");
            }
            if (!existing.getPhone().equals(registerRequest.getPhone())) {
                throw new IllegalArgumentException("Phone number must match your existing account");
            }
            if (!existing.getAddress().equalsIgnoreCase(registerRequest.getAddress())) {
                throw new IllegalArgumentException("Address must match your existing account");
            }
            if (!existing.getDateOfBirth().equals(registerRequest.getDateOfBirth())) {
                throw new IllegalArgumentException("Date of Birth must match your existing account");
            }
        }

        Account account = new Account();
        account.setHolderName(registerRequest.getHolderName());
        account.setEmail(registerRequest.getEmail());
        account.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.LOCKED);
        account.setApproved(false);
        account.setRole(Role.ROLE_USER);
        account.setAccountType(registerRequest.getAccountType());

        account.setPhone(registerRequest.getPhone());
        account.setAddress(registerRequest.getAddress());
        account.setDateOfBirth(registerRequest.getDateOfBirth());

        Account savedAccount = accountRepository.save(account);
        log.info("Account registered successfully: id={}, email={}, type={}", savedAccount.getId(),
                savedAccount.getEmail(), savedAccount.getAccountType());
        return savedAccount;
    }

    @Transactional
    public Account approveAccount(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        account.setApproved(true);
        account.setStatus(AccountStatus.ACTIVE);

        BigDecimal minimumBalance = accountProperties
                .getMinimumBalance(account.getAccountType().name());

        account.setBalance(minimumBalance);

        Account savedAccount = accountRepository.save(account);
        log.info("Account approved: id={}, email={}, initialBalance={}", savedAccount.getId(), savedAccount.getEmail(),
                minimumBalance);

        // Auto-link accounts with same email and holderName
        autoLinkIfApplicable(savedAccount);

        return savedAccount;
    }

    private void autoLinkIfApplicable(Account approvedAccount) {
        List<Account> sameEmailAccounts = accountRepository.findAllByEmail(approvedAccount.getEmail())
                .stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .filter(a -> a.getHolderName().equalsIgnoreCase(approvedAccount.getHolderName()))
                .toList();

        if (sameEmailAccounts.size() > 1) {
            Long firstId = sameEmailAccounts.get(0).getId();
            for (int i = 1; i < sameEmailAccounts.size(); i++) {
                try {
                    linkedAccountService.linkAccount(firstId, sameEmailAccounts.get(i).getId());
                } catch (RuntimeException ex) {
                    log.debug("Auto-link skipped: {}", ex.getMessage());
                }
            }
            log.info("Auto-linked {} accounts for email: {}", sameEmailAccounts.size(), approvedAccount.getEmail());
        }
    }

    @Transactional
    public Account rejectAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        account.setStatus(AccountStatus.CLOSED);
        Account rejected = accountRepository.save(account);
        log.info("Account rejected: id={}, email={}", accountId, account.getEmail());
        return rejected;
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
    }

    public Account getAccountByEmail(String email) {
        return accountRepository.findFirstByEmailOrderByCreatedAtAsc(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with email: " + email));
    }

    public List<Account> getPendingAccounts() {
        return accountRepository.findByApprovedFalse();
    }

    public List<Account> getActiveAccountsByEmail(String email) {
        return accountRepository.findAllByEmail(email)
                .stream()
                .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE)
                .toList();
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public void deposit(DepositRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero");
        }

        Account account = getAccountById(request.getAccountId());

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        // Log deposit transaction
        TransactionLog log = new TransactionLog();
        log.setToAccountId(account.getId());
        log.setAmount(request.getAmount());
        log.setTransactionType(TransactionType.DEPOSIT);
        log.setStatus(TransactionStatus.SUCCESS);
        transactionLogRepository.save(log);

        this.log.info("Admin deposited ₹{} to account ID: {}", request.getAmount(), request.getAccountId());
    }

    public BigDecimal getBalance(Long accountId) {
        Account account = getAccountById(accountId);
        return account.getBalance();
    }

    public List<AccountSearch> getSearchableAccounts(Long currentAccountId) {

        return accountRepository.findAll()
                .stream()
                .filter(acc -> !acc.getId().equals(1L)) // exclude system admin
                .filter(acc -> !acc.getId().equals(currentAccountId)) // exclude self
                .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE) // only active accounts

                .map(acc -> new AccountSearch(
                        acc.getId(),
                        acc.getHolderName()))
                .toList();
    }
}
