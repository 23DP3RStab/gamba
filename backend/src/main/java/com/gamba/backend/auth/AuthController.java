package com.gamba.backend.auth;

import com.gamba.backend.user.User;
import com.gamba.backend.user.UserService;
import com.gamba.backend.user.dto.LoginRequest;
import com.gamba.backend.user.dto.ProfileResponse;
import com.gamba.backend.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	public record AuthResponse(String token, ProfileResponse user) {
	}

	private final UserService userService;
	private final TokenService tokens;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return session(userService.register(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return session(userService.login(request.email(), request.password()));
	}

	private AuthResponse session(User user) {
		return new AuthResponse(tokens.issue(user), ProfileResponse.from(user));
	}

}
