package com.example.money_transfer_system.service;

import com.example.money_transfer_system.entity.Account;
import com.example.money_transfer_system.entity.LinkedAccount;
import com.example.money_transfer_system.repository.AccountRepository;
import com.example.money_transfer_system.repository.LinkedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedAccountService {

    private final LinkedAccountRepository linkedAccountRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void linkAccount(Long accountId1, Long accountId2) {

        if (accountId1.equals(accountId2)) {
            throw new RuntimeException("Cannot link an account to itself");
        }

        Account account1 = accountRepository.findById(accountId1)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId1));
        Account account2 = accountRepository.findById(accountId2)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId2));

        // Validate same holder name and email
        if (!account1.getHolderName().equalsIgnoreCase(account2.getHolderName()) ||
                !account1.getEmail().equalsIgnoreCase(account2.getEmail())) {
            throw new RuntimeException("Accounts must have the same holder name and email to be linked");
        }

        // Validate different account types
        if (account1.getAccountType() == account2.getAccountType()) {
            throw new RuntimeException("Accounts with the same type cannot be linked");
        }

        Optional<LinkedAccount> link1 = linkedAccountRepository.findByAccountId(accountId1);
        Optional<LinkedAccount> link2 = linkedAccountRepository.findByAccountId(accountId2);

        String groupId;

        if (link1.isPresent() && link2.isPresent()) {
            // Both already linked - check if same group
            if (link1.get().getGroupId().equals(link2.get().getGroupId())) {
                throw new RuntimeException("Accounts are already linked");
            }
            // Merge groups: move all of group2 into group1
            String targetGroup = link1.get().getGroupId();
            String sourceGroup = link2.get().getGroupId();
            List<LinkedAccount> toMerge = linkedAccountRepository.findByGroupId(sourceGroup);
            for (LinkedAccount la : toMerge) {
                la.setGroupId(targetGroup);
            }
            linkedAccountRepository.saveAll(toMerge);
            log.info("Merged group {} into group {}", sourceGroup, targetGroup);
            return;

        } else if (link1.isPresent()) {
            groupId = link1.get().getGroupId();
        } else if (link2.isPresent()) {
            groupId = link2.get().getGroupId();
        } else {
            groupId = UUID.randomUUID().toString();
        }

        // Create missing links
        if (link1.isEmpty()) {
            LinkedAccount newLink = new LinkedAccount();
            newLink.setGroupId(groupId);
            newLink.setAccountId(accountId1);
            linkedAccountRepository.save(newLink);
        }
        if (link2.isEmpty()) {
            LinkedAccount newLink = new LinkedAccount();
            newLink.setGroupId(groupId);
            newLink.setAccountId(accountId2);
            linkedAccountRepository.save(newLink);
        }

        log.info("Linked accounts {} and {} in group {}", accountId1, accountId2, groupId);
    }

    public List<Long> getLinkedAccountIds(Long accountId) {
        Optional<LinkedAccount> link = linkedAccountRepository.findByAccountId(accountId);
        if (link.isEmpty()) {
            return Collections.singletonList(accountId);
        }
        return linkedAccountRepository.findByGroupId(link.get().getGroupId())
                .stream()
                .map(LinkedAccount::getAccountId)
                .collect(Collectors.toList());
    }

    public boolean areLinked(Long accountId1, Long accountId2) {
        Optional<LinkedAccount> link1 = linkedAccountRepository.findByAccountId(accountId1);
        Optional<LinkedAccount> link2 = linkedAccountRepository.findByAccountId(accountId2);

        if (link1.isEmpty() || link2.isEmpty())
            return false;
        return link1.get().getGroupId().equals(link2.get().getGroupId());
    }

    public List<Account> getLinkedAccounts(Long accountId) {
        List<Long> ids = getLinkedAccountIds(accountId);
        return accountRepository.findAllById(ids)
                .stream()
                .filter(a -> !a.getId().equals(accountId))
                .collect(Collectors.toList());
    }

    public List<Account> getLinkableAccounts(Long accountId) {
        Account current = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        List<Long> alreadyLinked = getLinkedAccountIds(accountId);

        return accountRepository.findByHolderNameAndEmail(current.getHolderName(), current.getEmail())
                .stream()
                .filter(a -> !alreadyLinked.contains(a.getId()))
                .collect(Collectors.toList());
    }
}
