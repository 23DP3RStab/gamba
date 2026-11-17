package com.gamba.backend.service;

import com.gamba.backend.dto.BalanceResponse;
import com.gamba.backend.dto.PasswordRequest;
import com.gamba.backend.dto.ProfileRequest;
import com.gamba.backend.model.User;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// The logged-in user's own profile
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // Read fresh each time, so money added elsewhere (an admin, another tab) shows up
    @Transactional(readOnly = true)
    public BalanceResponse balance(UUID userId) {
        return BalanceResponse.from(walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found")));
    }

    @Transactional
    public User update(UUID userId, ProfileRequest request) {
        User user = get(userId);
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setBirthDate(request.getBirthDate());
        user.setEmail(email);
        user.setPhone(request.getPhone());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, PasswordRequest request) {
        User user = get(userId);

        if (!passwordEncoder.matches(request.getCurrent(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is wrong");
        }

        user.setPassword(passwordEncoder.encode(request.getNext()));
        userRepository.save(user);
    }

    // The wallet goes with the user (ON DELETE CASCADE)
    @Transactional
    public void delete(UUID userId) {
        userRepository.delete(get(userId));
    }
}
