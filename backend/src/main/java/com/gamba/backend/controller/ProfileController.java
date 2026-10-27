package com.gamba.backend.controller;

import com.gamba.backend.dto.*;
import com.gamba.backend.model.User;
import com.gamba.backend.service.GamblingLimitService;
import com.gamba.backend.service.ProfileService;
import com.gamba.backend.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// The logged-in user's own profile; JwtAuthenticationFilter puts that user in the principal
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final StatsService statsService;
    private final GamblingLimitService gamblingLimitService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved",
                ProfileResponse.from(profileService.get(user.getId()))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated",
                ProfileResponse.from(profileService.update(user.getId(), request))));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordRequest request
    ) {
        profileService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal User user) {
        profileService.delete(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile deleted", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Stats retrieved", statsService.getStats(user.getId())));
    }

    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<LimitsResponse>> limits(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Limits retrieved", gamblingLimitService.getLimits(user.getId())));
    }

    @PutMapping("/limits")
    public ResponseEntity<ApiResponse<LimitsResponse>> setLimit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody LossLimitRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Limit saved",
                gamblingLimitService.setLossLimit(user.getId(), request.getDailyLossLimit())));
    }

    @PostMapping("/lockout")
    public ResponseEntity<ApiResponse<LimitsResponse>> lockOut(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody LockoutRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Account locked",
                gamblingLimitService.lockOut(user.getId(), request.getDays())));
    }
}
