package com.gamba.backend.service;

import com.gamba.backend.dto.AuthResponse;
import com.gamba.backend.dto.LoginRequest;
import com.gamba.backend.dto.RegisterRequest;
import com.gamba.backend.model.Role;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import com.gamba.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PLAYER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.ZERO)
                .currency("EUR")
                .build();

        walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account has been suspended");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtService.generateToken(
                user.getUsername(),
                Map.of("role", user.getRole().name(), "userId", user.getId())
        );

        BigDecimal balance = BigDecimal.ZERO;
        String currency = "EUR";

        if (user.getWallet() != null) {
            balance = user.getWallet().getBalance();
            currency = user.getWallet().getCurrency();
        }

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .balance(balance)
                .currency(currency)
                .build();
    }
}