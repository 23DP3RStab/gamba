package com.gamba.backend.controller;

import com.gamba.backend.dto.AdminStatsDto;
import com.gamba.backend.dto.AdminUserDto;
import com.gamba.backend.dto.AdjustBalanceRequest;
import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Stats retrieved", adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved", adminService.getAllUsers()));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable UUID id) {
        adminService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", null));
    }

    @PostMapping("/users/{id}/balance")
    public ResponseEntity<ApiResponse<AdminUserDto>> adjustBalance(
            @PathVariable UUID id,
            @Valid @RequestBody AdjustBalanceRequest request
    ) {
        AdminUserDto updatedUser = adminService.adjustUserBalance(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Balance adjusted successfully", updatedUser));
    }
}