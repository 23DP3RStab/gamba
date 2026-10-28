package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.TransactionDto;
import com.gamba.backend.model.User;
import com.gamba.backend.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/transactions")
@RequiredArgsConstructor
public class WalletTransactionController {

    private final WalletTransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getMyTransactions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved", transactionService.getUserTransactions(user.getId())));
    }
}