package com.gamba.backend.service;

import com.gamba.backend.dto.AdminStatsDto;
import com.gamba.backend.dto.AdminUserDto;
import com.gamba.backend.dto.AdjustBalanceRequest;
import com.gamba.backend.dto.TransactionDto;
import com.gamba.backend.model.TransactionType;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;
    private final WalletLock walletLock;

    @Transactional(readOnly = true)
    public AdminStatsDto getDashboardStats() {
        List<User> allUsers = userRepository.findAll();
        List<Wallet> allWallets = walletRepository.findAll();

        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::isActive).count();
        long suspendedUsers = totalUsers - activeUsers;

        BigDecimal totalFunds = allWallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .totalPlayerFunds(totalFunds)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToAdminUserDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleUserStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional
    public AdminUserDto adjustUserBalance(UUID userId, AdjustBalanceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Wallet wallet = user.getWallet() == null ? null : walletLock.lock(user);
        if (wallet == null) {
            wallet = Wallet.builder()
                    .user(user)
                    .balance(BigDecimal.ZERO)
                    .currency("EUR")
                    .build();
        }

        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting balance cannot be negative");
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        String description = (request.getReason() != null && !request.getReason().trim().isEmpty())
                ? "Admin adjustment: " + request.getReason().trim()
                : "Admin manual balance adjustment";

        walletTransactionService.record(
                user,
                request.getAmount(),
                newBalance,
                TransactionType.ADMIN_ADJUSTMENT,
                description,
                null
        );

        return mapToAdminUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getUserTransactions(UUID userId) {
        return walletTransactionService.getUserTransactions(userId);
    }

    private AdminUserDto mapToAdminUserDto(User user) {
        BigDecimal balance = BigDecimal.ZERO;
        String currency = "EUR";

        if (user.getWallet() != null) {
            balance = user.getWallet().getBalance();
            currency = user.getWallet().getCurrency();
        }

        return AdminUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .balance(balance)
                .currency(currency)
                .createdAt(user.getCreatedAt())
                .build();
    }
}