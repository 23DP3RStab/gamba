package com.gamba.backend.service;

import com.gamba.backend.dto.TransactionDto;
import com.gamba.backend.model.TransactionType;
import com.gamba.backend.model.User;
import com.gamba.backend.model.WalletTransaction;
import com.gamba.backend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public void record(User user, BigDecimal amount, BigDecimal balanceAfter, TransactionType type, String description, UUID referenceId) {
        WalletTransaction tx = WalletTransaction.builder()
                .user(user)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .type(type)
                .description(description)
                .referenceId(referenceId)
                .build();

        transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getUserTransactions(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TransactionDto mapToDto(WalletTransaction tx) {
        return TransactionDto.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .type(tx.getType())
                .description(tx.getDescription())
                .referenceId(tx.getReferenceId())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}