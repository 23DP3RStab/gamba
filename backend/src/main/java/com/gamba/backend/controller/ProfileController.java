package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.PasswordRequest;
import com.gamba.backend.dto.ProfileRequest;
import com.gamba.backend.dto.ProfileResponse;
import com.gamba.backend.model.User;
import com.gamba.backend.service.ProfileService;
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
}
